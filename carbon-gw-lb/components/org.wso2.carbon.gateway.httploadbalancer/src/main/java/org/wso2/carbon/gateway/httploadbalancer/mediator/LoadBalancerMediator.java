package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalanceAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.RoundRobin;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;

/**
 * LoadBalancerMediator.
 * TODO: To be implemented.
 */
public class LoadBalancerMediator extends AbstractMediator {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediator.class);
    private String logMessage = "Message received at Load Balancer Mediator";

    private LoadBalanceAlgorithm lbAlgorithm;

    @Override
    public String getName() {

        return "LoadBalancerMediator";
    }

    public LoadBalancerMediator() {

    }

    public LoadBalancerMediator(List<OutboundEndpoint> outboundEndpoints, String algoName) {

        if (algoName.equals(LoadBalancerConstants.ROUND_ROBIN)) {

            lbAlgorithm = new RoundRobin(outboundEndpoints);
        } else {
            log.error("This algorithm is not supported as of now...");
            //TODO: Throw appropriate exception...
        }

    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        log.info(logMessage);
        //OutboundEndpoint endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage);
        //TODO: Call and respond mediator.
        return false;
    }
}
