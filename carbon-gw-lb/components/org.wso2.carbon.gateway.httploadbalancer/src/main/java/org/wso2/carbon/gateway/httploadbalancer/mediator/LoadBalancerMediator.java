package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
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

/**
 * LoadBalancerMediator.
 * TODO: To be implemented.
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


    public LoadBalancerMediator(List<OutboundEndpoint> outboundEndpoints, LoadBalancerConfigContext context) {

        this.context = context;
        lbMediatorMap = new ConcurrentHashMap<>();

        if (context.getAlgorithm().equals(LoadBalancerConstants.ROUND_ROBIN)) {

            lbAlgorithm = new RoundRobin(outboundEndpoints);
        }

        // Creating LoadBalancerCallMediators for OutboundEndpoints...
        for (OutboundEndpoint outboundEPs : outboundEndpoints) {
            lbMediatorMap.put(outboundEPs.getName(), new LoadBalancerCallMediator(outboundEPs, context));
        }

    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {

        log.info(logMessage);
        OutboundEndpoint endpoint = null;
        final String persistenceType = context.getPersistence();

        if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)
                || persistenceType.equals(LoadBalancerConstants.LB_COOKIE)) {

            String cookie = null;
            cookie = carbonMessage.getHeader(LoadBalancerConstants.COOKIE);

            // If There is no cookie of any kind and no LB specific cookie.
            if (cookie == null || !(cookie.contains(LoadBalancerConstants.COOKIE_PREFIX))) {

                //Fetching endpoint according to algorithm.
                endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);

            } else { //There is a LB specific cookie.

                String cookieName = null;

                //You are safe there is no other string similar to LoadBalancerConstants.COOKIE_PREFIX in this cookie.
                if (cookie.indexOf(LoadBalancerConstants.COOKIE_PREFIX) ==
                        cookie.lastIndexOf(LoadBalancerConstants.COOKIE_PREFIX)) {

                    int index = cookie.indexOf(LoadBalancerConstants.COOKIE_PREFIX);

                    //TODO: this logic is not safe.
                    cookieName = cookie.substring(index, index + 3);

                    String outboundEPKey = context.getOutboundEPKeyFromCookie(cookieName);


                    //TODO: For LB_COOKIE persistence type we have to check session timeout also.
                    //TODO: Remove LB specific cookie before forwarding req to server.


                    if (outboundEPKey != null) {

                        //Choosing endpoint based on persistence.
                        endpoint = context.getOutboundEndpoint(outboundEPKey);


                    } else {

                        log.error("Something went wrong. Persistence cannot be maintained.."
                                + "Choosing Endpoint based on algorithm");

                        //Fetching endpoint according to algorithm.
                        endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);

                    }

                } else { //Be careful some similar string is found. TODO: parsing logic.

                    //TODO: Change later.
                    //Fetching endpoint according to algorithm.
                    endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);
                }

            }

        } else if (persistenceType.equals(LoadBalancerConstants.CLIENT_IP_ADDRESS)) {

            log.info("Work in progress for this type...");
            //Fetching endpoint according to algorithm.
            endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);

        } else { //Policy is NO_PERSISTENCE

            //Fetching endpoint according to algorithm.
            endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);
        }

        log.info("Chosen endpoint by LB is.." + endpoint.getName());

        // Calling chosen OutboundEndpoint's LoadBalancerCallMediator's receive...
        lbMediatorMap.get(endpoint.getName()).
                receive(carbonMessage, new LoadBalancerMediatorCallBack(carbonCallback, this, context));


        return true;
    }
}
