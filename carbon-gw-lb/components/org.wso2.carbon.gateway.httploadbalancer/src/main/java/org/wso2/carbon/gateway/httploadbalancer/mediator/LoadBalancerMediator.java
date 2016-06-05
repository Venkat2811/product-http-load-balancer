package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.ClientIPHashing;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.RoundRobin;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.invokers.LoadBalancerCallMediator;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LoadBalancerMediator.
 */
public class LoadBalancerMediator extends AbstractMediator {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediator.class);
    private String logMessage = "Message received at Load Balancer Mediator";

    private Map<String, LoadBalancerCallMediator> lbMediatorMap;

    private LoadBalancingAlgorithm lbAlgorithm;
    private LoadBalancerConfigContext context;

    @Override
    public String getName() {

        return "LoadBalancerMediator";
    }


    /**
     *
     * @param outboundEndpoints OutboundEndpoints List.
     * @param context LoadBalancerConfigContext.
     */
    public LoadBalancerMediator(List<OutboundEndpoint> outboundEndpoints, LoadBalancerConfigContext context) {

        this.context = context;
        lbMediatorMap = new ConcurrentHashMap<>();

        if (context.getAlgorithm().equals(LoadBalancerConstants.ROUND_ROBIN)) {

            lbAlgorithm = new RoundRobin(outboundEndpoints);
        } else if (context.getAlgorithm().equals(LoadBalancerConstants.IP_HASHING)) {

            lbAlgorithm = new ClientIPHashing(outboundEndpoints);
        }

        // Creating LoadBalancerCallMediators for OutboundEndpoints...
        for (OutboundEndpoint outboundEPs : outboundEndpoints) {
            lbMediatorMap.put(outboundEPs.getName(), new LoadBalancerCallMediator(outboundEPs, context));
        }

    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {


         log.info("\n\n" + logMessage);
         log.info("Inside LB mediator...");
         Map<String, String> transHeaders = carbonMessage.getHeaders();
         log.info("Transport Headers...");
         log.info(transHeaders.toString() + "\n\n");

         Map<String, Object> prop = carbonMessage.getProperties();
         log.info("Properties...");
         log.info(prop.toString() + "\n\n");


        //log.info(" LB Mediator Cookie Header : " + carbonMessage.getHeader(LoadBalancerConstants.COOKIE_HEADER));

        OutboundEndpoint nextEndpoint = null;
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
                log.info("There is no LB specific cookie.." +
                        "Persistence cannot be maintained.." +
                        "Choosing Endpoint based on algorithm");
                nextEndpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);

            } else { //There is a LB specific cookie.

                String cookieName = null;


                if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)) {

                    boolean isError1 = false, isError2 = false;

                    //There are two possible cookie paterns in this case.
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
                        log.info("Couldn't retrieve LB Key from cookie of type 1 for " +
                                "Persistence type : " + LoadBalancerConstants.APPLICATION_COOKIE);
                    }

                    if (isError1) {
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
                            log.info("Couldn't retrieve LB Key from cookie of type 2 for " +
                                    "Persistence type :" + LoadBalancerConstants.APPLICATION_COOKIE);

                        }
                    }

                    if (isError1 && isError2) {
                        log.error("Cookie matching didn't match any of the two types for " +
                                "Persistence type :" + LoadBalancerConstants.APPLICATION_COOKIE);
                        return false;
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
                        return false;

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
                    nextEndpoint = context.getOutboundEndpoint(outboundEPKey);


                } else {

                    log.error("LB Key extraction using RegEx has gone for a toss." +
                            "Check the logic. " +
                            "Persistence cannot be maintained..");
                    return false;
                }


            }

        }  else { //Policy is NO_PERSISTENCE

            //Fetching endpoint according to algorithm.
            nextEndpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);
        }

        log.info("Chosen endpoint by LB is.." + nextEndpoint.getName());

        // Calling chosen OutboundEndpoint's LoadBalancerCallMediator's receive...
        lbMediatorMap.get(nextEndpoint.getName()).
                receive(carbonMessage, new LoadBalancerMediatorCallBack(carbonCallback, this, context));


        return true;
    }
}
