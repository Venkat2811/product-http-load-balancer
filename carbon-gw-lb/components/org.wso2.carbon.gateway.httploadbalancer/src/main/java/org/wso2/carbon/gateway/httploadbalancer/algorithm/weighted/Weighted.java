package org.wso2.carbon.gateway.httploadbalancer.algorithm.weighted;

import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;

import java.util.List;

/**
 * All Weighted Algorithms must implement this interface.
 */
public interface Weighted {

    /**
     *
     * @param lbOutboundEPs List of LBOutboundEndpoints
     * @param weights Their corresponding weights.
     *
     *                NOTE: All validations must be done before.
     *                      This method expects ordered list of
     *                      endpoints and their corresponding weights.
     */
    void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEPs, List<Integer> weights);


}
