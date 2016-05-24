package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;

/**
 * All LB algorithms must implement this interface.
 */
public interface LoadBalancingAlgorithm {

    /**
     * @return the name of implemented LB algorithm.
     */
    String getName();

    /**
     * @param outboundEPs list of all Outbound Endpoints to be load balanced.
     */
    void setOutboundEndpoints(List<OutboundEndpoint> outboundEPs);

    /**
     * @param cMsg Carbon Message has all headers required to make decision.
     * @return the next OutboundEndpoint according to implemented LB algorithm.
     */
    OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg);

    /**
     * Each implementation of LB algorithm will have certain values pertained to it.
     * (Eg: Round robin keeps track of index of OutboundEndpoint).
     * Implementation of this method will reset them.
     */
    void reset();

}
