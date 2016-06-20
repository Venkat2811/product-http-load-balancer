package org.wso2.carbon.gateway.httploadbalancer.algorithm.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
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


    private List<LBOutboundEndpoint> lbOutboundEndpoints;


    /**
     * @param lbOutboundEndpoints list of LBOutboundEndpoints to be load balanced.
     *                            <p>
     *                            EndpointsCount is also initialized here.
     */
    public RoundRobin(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        this.lbOutboundEndpoints = lbOutboundEndpoints;
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

        synchronized (this.lock) {
            this.lbOutboundEndpoints = lbOutboundEndpoints;

        }

    }


    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint to be added to the existing list.
     *                           <p>
     *                           EndpointsCount is also updated here.
     */
    @Override
    public void addLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (this.lock) {
            if (!this.lbOutboundEndpoints.contains(lbOutboundEndpoint)) {
                this.lbOutboundEndpoints.add(lbOutboundEndpoint);

            } else {
                log.error(lbOutboundEndpoint.getName() + " already exists in list..");
            }
        }

    }

    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint to be removed from existing list.
     *                           <p>
     *                           EndpointsCount is also updated here.
     */
    @Override
    public void removeLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (this.lock) {
            if (this.lbOutboundEndpoints.contains(lbOutboundEndpoint)) {
                this.lbOutboundEndpoints.remove(lbOutboundEndpoint);

            } else {
                log.error(lbOutboundEndpoint.getName() + " is not in list..");
            }
        }
    }

    /**
     * For getting next LBOutboundEndpoinnt.
     * This method is called after locking, so don't worry.
     */
    private void incrementIndex() {

        this.index++;
        this.index %= this.lbOutboundEndpoints.size();
    }

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return chosen OutboundEndpoint
     */

    @Override
    public LBOutboundEndpoint getNextLBOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        LBOutboundEndpoint endPoint = null;

        synchronized (this.lock) {
            if (this.lbOutboundEndpoints != null && this.lbOutboundEndpoints.size() > 0) {

                endPoint = this.lbOutboundEndpoints.get(this.index);
                incrementIndex();

            } else {

                log.error("No OutboundEndpoint is available..");

            }
        }

        return endPoint;
    }

    /**
     * Resets the index.
     */
    @Override
    public void reset() {

        synchronized (this.lock) {

            if (this.lbOutboundEndpoints.size() > 0 && this.index >= this.lbOutboundEndpoints.size()) {
                this.index %= this.lbOutboundEndpoints.size();
            }
        }
    }

    @Override
    public Object getLock() {

        return this.lock;
    }
}
