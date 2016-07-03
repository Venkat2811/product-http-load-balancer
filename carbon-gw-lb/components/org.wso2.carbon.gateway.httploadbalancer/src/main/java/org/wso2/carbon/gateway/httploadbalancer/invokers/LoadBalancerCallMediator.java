package org.wso2.carbon.gateway.httploadbalancer.invokers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.simple.LeastResponseTime;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.weighted.WeightedRandom;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.weighted.WeightedRoundRobin;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;


/**
 * CallMediator for LoadBalancer.
 */
public class LoadBalancerCallMediator extends AbstractMediator {


    private LBOutboundEndpoint lbOutboundEndpoint;

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerCallMediator.class);

    private final LoadBalancerConfigContext context;


    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint.
     * @param context            LoadBalancerConfigContext.
     */
    public LoadBalancerCallMediator(LBOutboundEndpoint lbOutboundEndpoint,
                                    LoadBalancerConfigContext context) {


        this.lbOutboundEndpoint = lbOutboundEndpoint;
        this.context = context;
    }


    @Override
    public String getName() {
        return "LoadBalancerCallMediator";
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws Exception {

        /**
         log.info("Inside LB call mediator...");

         log.info("Transport Headers...");
         log.info(carbonMessage.getHeaders().toString());

         log.info("Properties...");
         log.info(carbonMessage.getProperties().toString());
         **/

            //Using separate LBMediatorCallBack because, we are handling headers in CallBack for session persistence.
        CarbonCallback  callback = new LoadBalancerMediatorCallBack(carbonCallback, this,
                    this.context, this.lbOutboundEndpoint);



        // We are doing this because, in the following algorithms we are using WINDOW.
        if (context.getAlgorithmName().equals(LoadBalancerConstants.LEAST_RESPONSE_TIME)) {

            ((LeastResponseTime) context.getLoadBalancingAlgorithm()).
                    receive(carbonMessage, callback, this.context, this.lbOutboundEndpoint);

        } else if (context.getAlgorithmName().equals(LoadBalancerConstants.WEIGHTED_ROUND_ROBIN)) {

            ((WeightedRoundRobin) context.getLoadBalancingAlgorithm()).
                    receive(carbonMessage, callback, this.context, this.lbOutboundEndpoint);

        } else if (context.getAlgorithmName().equals(LoadBalancerConstants.WEIGHTED_RANDOM)) {

            ((WeightedRandom) context.getLoadBalancingAlgorithm()).
                    receive(carbonMessage, callback, this.context, this.lbOutboundEndpoint);
        } else {
            lbOutboundEndpoint.receive(carbonMessage, callback, this.context);
        }

        return false;
    }
}
