package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.simple.RoundRobin;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.simple.StrictClientIPHashing;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.invokers.LoadBalancerCallMediator;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
import org.wso2.carbon.gateway.httploadbalancer.utils.handlers.error.LBErrorHandler;

import org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled.TimeoutHandler;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LoadBalancerMediator.
 * <p>
 * This mediator receives client's request from InboundOutbound endpoint's pipeline.
 * <p>
 * This is responsible to choose Outbound endpoint based on the specified algorithm choice.
 * <p>
 * This mediator will also look for headers in client request to maintain persistence.
 * <p>
 * This mediator is responsible for choosing healthy OutboundEndpoint.
 */
public class LoadBalancerMediator extends AbstractMediator {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediator.class);
    private final String logMessage = "Message received at Load Balancer Mediator";

    private Map<String, LoadBalancerCallMediator> lbCallMediatorMap;

    private final LoadBalancingAlgorithm lbAlgorithm;
    private final LoadBalancerConfigContext context;

    private String configName;


    @Override
    public String getName() {

        return "LoadBalancerMediator";
    }


    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    /**
     * @param lbOutboundEndpoints LBOutboundEndpoints List.
     * @param context             LoadBalancerConfigContext.
     */
    public LoadBalancerMediator(List<LBOutboundEndpoint> lbOutboundEndpoints,
                                LoadBalancerConfigContext context, String configName) {

        this.context = context;
        this.configName = configName;
        lbCallMediatorMap = new ConcurrentHashMap<>();

        if (context.getAlgorithm().equals(LoadBalancerConstants.ROUND_ROBIN)) {

            lbAlgorithm = new RoundRobin(lbOutboundEndpoints);

            if (context.getPersistence().equals(LoadBalancerConstants.CLIENT_IP_HASHING)) {

                context.initStrictClientIPHashing(lbOutboundEndpoints);
            }

        } else if (context.getAlgorithm().equals(LoadBalancerConstants.STRICT_IP_HASHING)) {

            lbAlgorithm = new StrictClientIPHashing(lbOutboundEndpoints);

        } else {
            lbAlgorithm = null;
            return;
        }

        // Creating LoadBalancerCallMediators for OutboundEndpoints...
        for (LBOutboundEndpoint lbOutboundEP : lbOutboundEndpoints) {
            lbCallMediatorMap.put(lbOutboundEP.getName(), new LoadBalancerCallMediator(lbOutboundEP, context));
        }

        //At this point everything is initialized.


        /**
         * TimeoutHandler.
         */
        TimeoutHandler timeoutHandler = new TimeoutHandler(this.context, this.lbAlgorithm, this.configName);

        Timer timerForTimeout = new Timer(timeoutHandler.getHandlerName(), true);

        timerForTimeout.schedule(timeoutHandler, 0, LoadBalancerConstants.DEFAULT_TIMER_PERIOD); //TODO: time


        //TODO: are two timers okay..?
        /**
         * BackToHealthyHandler.
         *
         * This is started only if health check is enabled.
         */
        /**    if (!this.context.getHealthCheck().equals(LoadBalancerConstants.NO_HEALTH_CHECK)) {
         BackToHealthyHandler backToHealthyHandler = new BackToHealthyHandler(this.context,
         this.lbAlgorithm, this.lbCallMediatorMap, this.configName);

         Timer timerForHealthyHandler = new Timer(backToHealthyHandler.getHandlerName(), true);

         timerForHealthyHandler.schedule(backToHealthyHandler, 0,
         context.getHealthycheckInterval()); //TODO: time
         }**/


    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {

        log.info(logMessage);


/**
 log.info("Inside LB mediator...");

 log.info("Transport Headers...");
 log.info(carbonMessage.getHeaders() + "\n\n");

 log.info("Properties...");
 log.info(carbonMessage.getProperties() + "\n\n");

 **/


        //log.info(" LB Mediator Cookie Header : " + carbonMessage.getHeader(LoadBalancerConstants.COOKIE_HEADER));

        LBOutboundEndpoint nextLBOutboundEndpoint = null;
        final String persistenceType = context.getPersistence();


        if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)
                || persistenceType.equals(LoadBalancerConstants.LB_COOKIE)) {

            String existingCookie = null;

            //Getting cookie from request header.
            existingCookie = carbonMessage.getHeader(LoadBalancerConstants.COOKIE_HEADER);

            /**NOTE: You can maintain persistence only if you have LB specific cookie.**/

            if (existingCookie == null || !(existingCookie.contains(LoadBalancerConstants.LB_COOKIE_NAME))) {
                //There is no cookie or no LB specific cookie.

                //Fetching endpoint according to algorithm (no persistence is maintained).
                log.warn("There is no LB specific cookie.." +
                        "Persistence cannot be maintained.." +
                        "Choosing Endpoint based on algorithm");
                nextLBOutboundEndpoint = lbAlgorithm.getNextLBOutboundEndpoint(carbonMessage, context);

            } else { //There is a LB specific cookie.

                String cookieName = null;


                if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)) {

                    boolean isError1 = false, isError2 = false;

                    //There are two possible cookie paterns in this case.
                    /**
                     * TYPE 1 FORMAT
                     */
                    //1) eg cookie: JSESSIONID=ghsgsdgsg---LB_COOKIE:EP1---
                    // We need to retrieve EP1 in this case.
                    String regEx =
                            "(" +
                                    LoadBalancerConstants.LB_COOKIE_DELIMITER +
                                    LoadBalancerConstants.LB_COOKIE_NAME +
                                    LoadBalancerConstants.COOKIE_NAME_VALUE_SEPARATOR +
                                    ")" +
                                    "(.*)" +
                                    "(" +
                                    LoadBalancerConstants.LB_COOKIE_DELIMITER +
                                    ")";

                    Pattern p = Pattern.compile(regEx);
                    Matcher m = p.matcher(existingCookie);
                    if (m.find()) {
                        cookieName = m.group(2);
                    } else {
                        isError1 = true;
                        log.warn("Couldn't retrieve LB Key from cookie of type 1 for " +
                                "Persistence type : " + LoadBalancerConstants.APPLICATION_COOKIE);
                    }

                    if (isError1) {
                        /**
                         * TYPE 2 FORMAT
                         */
                        // 2) cookie: LB_COOKIE=EP1
                        // We need to retrieve EP1 in this case.
                        regEx = "(" +
                                LoadBalancerConstants.LB_COOKIE_NAME +
                                "=)(.*)";
                        p = Pattern.compile(regEx);
                        m = p.matcher(existingCookie);

                        if (m.find()) {
                            cookieName = m.group(2);

                        } else {
                            isError2 = true;
                            log.warn("Couldn't retrieve LB Key from cookie of type 2 for " +
                                    "Persistence type :" + LoadBalancerConstants.APPLICATION_COOKIE);

                        }
                    }

                    if (isError1 && isError2) {

                        log.error("Cookie matching didn't match any of the two types for " +
                                "Persistence type :" + LoadBalancerConstants.APPLICATION_COOKIE);

                        //TODO: is this okay or should we send error..?
                        log.error("Persistence cannot be maintained.. Endpoint will be chosen based on algorithm.");


                    } else if (isError1) {

                        log.info("LB key of type 2 has been retrieved from cookie for" +
                                "Persistence type :" + LoadBalancerConstants.APPLICATION_COOKIE);
                    }


                } else { // persistenceType is LoadBalancerConstants.LB_COOKIE.

                    //eg cookie: LB_COOKIE=EP1
                    //We need to retrieve EP1 in this case.
                    String regEx = "(" +
                            LoadBalancerConstants.LB_COOKIE_NAME +
                            "=)(.*)";

                    Pattern p = Pattern.compile(regEx);
                    Matcher m = p.matcher(existingCookie);
                    if (m.find()) {
                        cookieName = m.group(2);
                    } else {

                        log.error("Couldn't retrieve LB Key from cookie for " +
                                "Persistence type :" + LoadBalancerConstants.LB_COOKIE);

                        //TODO: is this okay or should we send error..?
                        log.error("Persistence cannot be maintained.. Endpoint will be chosen based on algorithm.");

                    }

                }


                if (context.getOutboundEPKeyFromCookie(cookieName) != null) {

                    String outboundEPKey = context.getOutboundEPKeyFromCookie(cookieName);


                    /** Removing LB specific cookie before forwarding req to server. */

                    //If there is delimiter, there exists a BE server's cookie.
                    if (existingCookie.contains(LoadBalancerConstants.LB_COOKIE_DELIMITER)) {

                        //Removing our LB specific cookie from BE cookie. We don't want it be sent to BE.
                        existingCookie = existingCookie.substring(0,
                                existingCookie.indexOf(LoadBalancerConstants.LB_COOKIE_DELIMITER));

                        carbonMessage.setHeader(LoadBalancerConstants.COOKIE_HEADER, existingCookie);

                    } else {

                        //There is only LB specific cookie. We don't want it to be sent to BE.
                        carbonMessage.removeHeader(LoadBalancerConstants.COOKIE_HEADER);
                    }


                    //Choosing endpoint based on persistence.
                    nextLBOutboundEndpoint = context.getLBOutboundEndpoint(outboundEPKey);


                } else {

                    log.error("LB Key extraction using RegEx has gone for a toss." +
                            "Check the logic. ");

                    //TODO: is this okay or should we send error..?
                    log.error("Persistence cannot be maintained.. Choosing endpoint based on algorithm.");

                    //Fetching endpoint according to algorithm.
                    nextLBOutboundEndpoint = lbAlgorithm.getNextLBOutboundEndpoint(carbonMessage, context);
                }


            }

        } else if (persistenceType.equals(LoadBalancerConstants.CLIENT_IP_HASHING)) {

            String ipAddress = CommonUtil.getClientIP(carbonMessage);
            log.info("IP address retrieved is : " + ipAddress);
            if (CommonUtil.isValidIP(ipAddress)) {

                //Chosing endpoint based on IP Hashing.
                nextLBOutboundEndpoint = context.getLBOutboundEndpoint(
                        context.getStrictClientIPHashing().getHash().get(ipAddress));

            } else {

                log.error("The IP Address retrieved is : " + ipAddress +
                        " which is invalid according to our validation. " +
                        "Endpoint will be chosen based on algorithm");
                //TODO: throw appropriate exceptions also.
                //Fetching endpoint according to algorithm.
                nextLBOutboundEndpoint = lbAlgorithm.getNextLBOutboundEndpoint(carbonMessage, context);

            }

        } else { //Policy is NO_PERSISTENCE

            //Fetching endpoint according to algorithm.
            nextLBOutboundEndpoint = lbAlgorithm.getNextLBOutboundEndpoint(carbonMessage, context);


        }

        if (nextLBOutboundEndpoint != null) {
            log.info("Chosen endpoint by LB is.." + nextLBOutboundEndpoint.getName());

            /**
             *  NOTE: The places where LBOutboundEndpoint's properties will be changed are:
             *  1) LoadBalancerMediatorCallBack
             *  2) TimeoutHandler
             *
             *  We are acquiring lock on respective LBOutboundEndpoint object in the above mentioned
             *  classes when the properties are being changed. So here we need not worry about locking
             *  here because here we are just reading them.
             *
             *  NOTE: In case of NO_HEALTH_CHECK, endpoints will always be healthy but timeOut checking will happen.
             *  So, if condition will always be true.
             *
             *  The returned endpoint by algorithm will always be healthy, but we are making the below
             *  check just to be safe.
             */
            if (nextLBOutboundEndpoint.isHealthy()) {
                // Chosen Endpoint is healthy.
                // If there is any persistence, it will be maintained.

                // Calling chosen LBOutboundEndpoint's LoadBalancerCallMediator receive...

                lbCallMediatorMap.get(nextLBOutboundEndpoint.getName()).
                        receive(carbonMessage, new LoadBalancerMediatorCallBack(carbonCallback, this,
                                this.context, nextLBOutboundEndpoint));
                return true;

            } else {

                while (true) {
                    /**
                     * Here we are trying to fetch healthy endpoint.
                     *
                     * This loop also handles if there is no healthy endpoint available.
                     *
                     * Adding and removing unHealthyEndpoint to the UnHealthyList happens in TimeoutHandler not here.
                     *
                     * If there is any persistence, it WILL NOT BE MAINTAINED because the already chosen endpoint
                     * with persistence is unHealthy and we again don't want to chose it.
                     */

                    //Fetching endpoint according to algorithm.
                    nextLBOutboundEndpoint = lbAlgorithm.getNextLBOutboundEndpoint(carbonMessage, context);

                    //Here null pointer test is must.
                    if (nextLBOutboundEndpoint != null) {

                        if (nextLBOutboundEndpoint.isHealthy()) { //The new Chosen Endpoint is healthy.

                            log.info("Previously chosen endpoint is unHealthy.. Now, Chosen endpoint is : " +
                                    nextLBOutboundEndpoint.getName());

                            // Calling chosen LBOutboundEndpoint's LoadBalancerCallMediator receive...
                            lbCallMediatorMap.get(nextLBOutboundEndpoint.getName()).
                                    receive(carbonMessage, new LoadBalancerMediatorCallBack(carbonCallback, this,
                                            this.context, nextLBOutboundEndpoint));
                            return true;

                        } else {

                            if (areAllEndpointsUnhealthy()) {

                                sendResponse(carbonCallback, false);
                                return false;
                            }
                        }

                    } else { //If algorithm returns null, there is no endpoint available.

                        sendResponse(carbonCallback, false);
                        return false;
                    }

                }
            }
        } else {

            if (areAllEndpointsUnhealthy()) {

                sendResponse(carbonCallback, false);
            } else {

                log.error("Unable to choose endpoint for forwarding the request." +
                        " Check logs to see what went wrong.");
                sendResponse(carbonCallback, true);
            }
            return false;
        }


    }

    private boolean areAllEndpointsUnhealthy() {

        int unHealthyListSize;

        // Here locking is required as we are fetching size.
        synchronized (this.context.getUnHealthyLBEPQueue()) {
            unHealthyListSize = context.getUnHealthyEPQueueSize();
        }

        if (context.getLbOutboundEndpoints().size() == unHealthyListSize) {
            return true;
        } else {
            return false;
        }

    }

    private void sendResponse(CarbonCallback carbonCallback, boolean isInternalError) throws Exception {

        if (!isInternalError) {
            log.error("All LBOutboundEndpoints are unHealthy..");
            new LBErrorHandler().handleFault
                    ("503", new Throwable("Service Unavailable.. " +
                            "Kindly try after some time.."), null, carbonCallback);
        } else {

            new LBErrorHandler().handleFault
                    ("500", new Throwable("Internal Server Error.."), null, carbonCallback);

        }


    }


}
