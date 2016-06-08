package org.wso2.carbon.gateway.httploadbalancer.algorithm.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;
import java.util.List;


/**
 * Implementation of Round Robin Algorithm.
 * <p>
 * All Endpoints are assumed to have equal weights.
 */
public class RoundRobin implements LoadBalancingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(RoundRobin.class);
    private final Object lock = new Object();

    private int index = 0;
    private int endPointsCount = 0;

    private List<LBOutboundEndpoint> lbOutboundEndpoints;


    /**
     * @param lbOutboundEndpoints list of LBOutboundEndpoints to be load balanced.
     *                            <p>
     *                            EndpointsCount is also initialized here.
     */
    public RoundRobin(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        synchronized (lock) {
            this.lbOutboundEndpoints = lbOutboundEndpoints;
            endPointsCount = lbOutboundEndpoints.size();
        }
    }


    /**
     * @return Algorithm name.
     */
    @Override
    public String getName() {

        return LoadBalancerConstants.ROUND_ROBIN;
    }

    /**
     * @param lbOutboundEndpoints list of LBOutboundEndpoints to be load balanced.
     *                            <p>
     *                            EndpointsCount is also initialized here.
     */
    @Override
    public void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        synchronized (lock) {
            this.lbOutboundEndpoints = lbOutboundEndpoints;
            endPointsCount = lbOutboundEndpoints.size();
        }

    }


    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint to be added to the existing list.
     *                           <p>
     *                           EndpointsCount is also updated here.
     */
    @Override
    public void addLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (lock) {
            lbOutboundEndpoints.add(lbOutboundEndpoint);
            endPointsCount = lbOutboundEndpoints.size();
        }

    }

    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint to be removed from existing list.
     *                           <p>
     *                           EndpointsCount is also updated here.
     */
    @Override
    public void removeLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (lock) {
            lbOutboundEndpoints.remove(lbOutboundEndpoint);
            endPointsCount = lbOutboundEndpoints.size();
        }
    }

    /**
     * For getting next LBOutboundEndpoinnt.
     * This method is called after locking, so don't worry.
     */
    private void incrementIndex() {

        index++;
        index %= endPointsCount;
    }

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return chosen OutboundEndpoint
     */

    @Override
    public LBOutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        LBOutboundEndpoint endPoint = null;

        synchronized (lock) {
            if (lbOutboundEndpoints != null && lbOutboundEndpoints.size() > 0) {

                endPoint = lbOutboundEndpoints.get(index);
                incrementIndex();

            } else {

                log.error("No OutboundEndpoint is available..");
                //TODO: throw appropriate exceptions also.

            }
        }

        return endPoint;
    }

    /**
     * Resets the index.
     */
    @Override
    public void reset() {

        synchronized (lock) {
            index = 0;
        }
    }
}
