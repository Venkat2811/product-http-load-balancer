package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
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

    private List<OutboundEndpoint> outboundEndpoints;


    /**
     * Default Constructor.
     */
    public RoundRobin() {

    }

    /**
     * @param outboundEndpoints list of outboundEndpoints to be load balanced.
     *                          <p>
     *                          EndpointsCount is also initialized here.
     */
    public RoundRobin(List<OutboundEndpoint> outboundEndpoints) {

        synchronized (lock) {
            this.outboundEndpoints = outboundEndpoints;
            endPointsCount = outboundEndpoints.size();
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
     * @param outboundEndpoints list of outboundEndpoints to be load balanced.
     *                          <p>
     *                          EndpointsCount is also initialized here.
     */
    @Override
    public void setOutboundEndpoints(List<OutboundEndpoint> outboundEndpoints) {

        synchronized (lock) {
            this.outboundEndpoints = outboundEndpoints;
            endPointsCount = outboundEndpoints.size();
        }

    }


    /**
     * @param outboundEndpoint outboundEndpoint to be added to the existing list.
     *                         <p>
     *                         EndpointsCount is also updated here.
     */
    @Override
    public void addOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        synchronized (lock) {
            outboundEndpoints.add(outboundEndpoint);
            endPointsCount = outboundEndpoints.size();
        }

    }

    /**
     * @param outboundEndpoint outboundEndpoint to be removed from existing list.
     *                         <p>
     *                         EndpointsCount is also updated here.
     */
    @Override
    public void removeOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        synchronized (lock) {
            outboundEndpoints.remove(outboundEndpoint);
            endPointsCount = outboundEndpoints.size();
        }
    }

    /**
     * For getting next OutboundEndpoinnt.
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
     * <p>
     * <p>
     * TODO: Before choosing an endpoint check whether it is healthy or not.
     */

    @Override
    public OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        OutboundEndpoint endPoint = null;

        synchronized (lock) {
            if (outboundEndpoints != null && outboundEndpoints.size() > 0) {

                endPoint = outboundEndpoints.get(index);
                incrementIndex();

            } else {

                log.error("No outbound end point is available..");
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
