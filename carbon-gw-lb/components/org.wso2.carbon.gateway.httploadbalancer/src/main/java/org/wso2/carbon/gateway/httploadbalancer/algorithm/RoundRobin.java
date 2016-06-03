package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of Round Robin Algorithm.
 * All Endpoints are assumed to have equal weights.
 * TODO: Is re-entrant lock okay..?
 */
public class RoundRobin implements LoadBalancingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(RoundRobin.class);
    private final Lock lock = new ReentrantLock();

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
        lock.lock();
        try {
            this.outboundEndpoints = outboundEndpoints;
            endPointsCount = outboundEndpoints.size();
        } finally {
            lock.unlock();
        }
    }


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

        lock.lock();
        try {
            this.outboundEndpoints = outboundEndpoints;
            endPointsCount = outboundEndpoints.size();
        } finally {
            lock.unlock();
        }

    }


    /**
     * @param outboundEndpoint outboundEndpoint to be added to the existing list.
     *                         <p>
     *                         EndpointsCount is also updated here.
     */
    @Override
    public void addOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        lock.lock();
        try {
            outboundEndpoints.add(outboundEndpoint);
            endPointsCount = outboundEndpoints.size();
        } finally {
            lock.unlock();
        }

    }

    /**
     * @param outboundEndpoint outboundEndpoint to be removed from existing list.
     *                         <p>
     *                         EndpointsCount is also updated here.
     */
    @Override
    public void removeOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        lock.lock();
        try {
            outboundEndpoints.remove(outboundEndpoint);
            endPointsCount = outboundEndpoints.size();
        } finally {
            lock.unlock();
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
     * @param cMsg Carbon Message has all headers required to make decision.
     * @return chosen Outboundendpoint
     * <p>
     * TODO: Support persistence also. Before choosing an endpoint check whether it is healthy or not.
     */

    @Override
    public OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        OutboundEndpoint endPoint = null;

        lock.lock();
        try {
            if (outboundEndpoints != null && outboundEndpoints.size() > 0) {

                endPoint = outboundEndpoints.get(index);
                incrementIndex();

            } else {

                log.error("No outbound end point is available..");
                //TODO: throw appropriate exceptions also.

            }
        } finally {
            lock.unlock();
        }


        return endPoint;
    }

    @Override
    public void reset() {

        lock.lock();
        try {
            index = 0;
        } finally {
            lock.unlock();
        }
    }
}
