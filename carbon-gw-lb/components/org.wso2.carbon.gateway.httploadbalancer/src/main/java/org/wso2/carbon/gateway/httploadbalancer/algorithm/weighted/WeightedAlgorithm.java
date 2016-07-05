package org.wso2.carbon.gateway.httploadbalancer.algorithm.weighted;

import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;

/**
 * All Weighted Algorithms must implement this interface.
 */
public interface WeightedAlgorithm extends LoadBalancingAlgorithm {

    /**
     * @param lbOutboundEPs List of LBOutboundEndpoints
     * @param weights       Their corresponding weights.
     *                      <p>
     *                      NOTE: All validations must be done before.
     *                      This method expects ordered list of
     *                      endpoints and their corresponding weights.
     */
    void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEPs, List<Integer> weights);

    /**
     * @param carbonMessage      CarbonMessage
     * @param carbonCallback     CarbonCallback
     * @param context            LoadBalancerConfigContext
     * @param lbOutboundEndpoint LBOutboundEndpoint
     * @return
     * @throws Exception NOTE: Unlike Simple algorithms, in case if decision is made based on persistence,
     *                   algorithm should know that request is being sent to an endpoint.  So that it
     *                   can increment window tracker and weighted load distribution can be done in
     *                   a much efficient way.
     */
    boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                    LoadBalancerConfigContext context,
                    LBOutboundEndpoint lbOutboundEndpoint) throws Exception;


}
