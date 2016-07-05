package org.wso2.carbon.gateway.httploadbalancer.algorithm;


import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;



/**
 * All types of LB algorithms must implement this interface.
 * Algorithm implementation MUST ensure that all the operations are THREAD SAFE.
 */
public interface LoadBalancingAlgorithm {

    /**
     * @return the name of implemented LB algorithm.
     */
    String getName();


    /**
     * @param lbOutboundEndpoint outboundEndpoint to be added to the existing list.
     *                           <p>
     *                           This method will be used to add an endpoint once it
     *                           is back to healthy state.
     */
    void addLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint);

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be removed from existing list.
     *                           <p>
     *                           This method will be used to remove an unHealthyEndpoint.
     */
    void removeLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint);

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return the next LBOutboundEndpoint according to implemented LB algorithm.
     */

    LBOutboundEndpoint getNextLBOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context);

    /**
     * Each implementation of LB algorithm will have certain values pertained to it.
     * (Eg: Round robin keeps track of index of OutboundEndpoint).
     * Implementation of this method will resetHealthPropertiesToDefault.
     */
    void reset();

    /**
     * @return Object used for locking.
     */
    Object getLock();

}
