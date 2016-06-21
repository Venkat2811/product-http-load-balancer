package org.wso2.carbon.gateway.httploadbalancer.algorithm.simple;

import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;

import java.util.List;

/**
 * All Simple Algorithms must implement this interface.
 */
public interface Simple {

    /**
     * @param lbOutboundEPs list of all Outbound Endpoints to be load balanced.
     */
    void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEPs);
}
