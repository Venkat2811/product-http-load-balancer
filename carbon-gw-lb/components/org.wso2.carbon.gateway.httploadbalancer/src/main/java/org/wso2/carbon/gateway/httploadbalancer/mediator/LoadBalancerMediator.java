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
            lbMediatorMap.put(outboundEPs.getName(), new LoadBalancerCallMediator(outboundEPs));
        }

    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {

        log.info(logMessage);
        OutboundEndpoint endpoint = lbAlgorithm.getNextOutboundEndpoint(carbonMessage, context);
        log.info("Chosen endpoint by LB is.." + endpoint.getName());

        // Calling chosen OutboundEndpoint's LoadBalancerCallMediator's receive...
        lbMediatorMap.get(endpoint.getName()).
                receive(carbonMessage, new LoadBalancerMediatorCallBack(carbonCallback, this));


        return true;
    }
}
