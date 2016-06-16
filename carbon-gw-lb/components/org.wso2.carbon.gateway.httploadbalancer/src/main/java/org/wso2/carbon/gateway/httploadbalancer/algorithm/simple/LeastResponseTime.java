package org.wso2.carbon.gateway.httploadbalancer.algorithm.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;


import java.util.Comparator;
import java.util.List;

/**
 * Implementation of LeastResponseTime.
 * <p>
 * All Endpoints are assumed to have equal weights.
 * <p>
 * TODO: To be implemented.
 */
public class LeastResponseTime implements LoadBalancingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(LeastResponseTime.class);
    private final Object lock = new Object();

    private int index = 0;
    public static final int SORT_WINDOW = 10;

    private List<LBOutboundEndpoint> lbOutboundEndpoints;

    private volatile LBOutboundEndpoint prevPointer;
    private volatile LBOutboundEndpoint firstPointer;


    /**
     * Constructor.
     *
     * @param lbOutboundEndpoints
     */
    public LeastResponseTime(List<LBOutboundEndpoint> lbOutboundEndpoints) {
        this.lbOutboundEndpoints = lbOutboundEndpoints;

        //Before Sorting.
        prevPointer = this.lbOutboundEndpoints.get(0);
        firstPointer = this.lbOutboundEndpoints.get(0);

    }


    /**
     * @return the name of implemented LB algorithm.
     */
    @Override
    public String getName() {

        return LoadBalancerConstants.LEAST_RESPONSE_TIME;
    }

    /**
     * @param lbOutboundEPs list of all Outbound Endpoints to be load balanced.
     */
    @Override
    public void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEPs) {

        synchronized (this.lock) {
            this.lbOutboundEndpoints = lbOutboundEPs;

        }
    }

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be added to the existing list.
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
     * @param lbOutboundEndpoint outboundEndpoint to be removed from existing list.
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

    private void computeNextEndpoint() {

        firstPointer = lbOutboundEndpoints.get(0);
        //Sorting based on AverageResponseTime.
        this.lbOutboundEndpoints.sort(Comparator.comparing(LBOutboundEndpoint::getAvgResponseTime));

        // Purpose of sorting is to change, if it doesn't we have to change it for the
        // sake of even load distribution.
        if (firstPointer == lbOutboundEndpoints.get(0)) {

            lbOutboundEndpoints.remove(firstPointer);
            lbOutboundEndpoints.add(firstPointer);

            if (prevPointer != lbOutboundEndpoints.get(0)) {
                prevPointer = firstPointer;
                firstPointer = lbOutboundEndpoints.get(0);
            } else {

                lbOutboundEndpoints.remove(prevPointer);
                lbOutboundEndpoints.add(prevPointer);
                firstPointer = lbOutboundEndpoints.get(0);
                prevPointer = firstPointer;
            }
        }

    }

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return the next LBOutboundEndpoint according to implemented LB algorithm.
     */
    @Override
    public LBOutboundEndpoint getNextLBOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        LBOutboundEndpoint endPoint = null;

        synchronized (this.lock) {
            if (this.lbOutboundEndpoints != null && this.lbOutboundEndpoints.size() > 0) {

                // Frequent sorting will be an overkill.
                if (this.lbOutboundEndpoints.size() > 1 && this.index >= SORT_WINDOW) {
                    computeNextEndpoint();
                    this.index = 0;
                }

                for (LBOutboundEndpoint endpoint : this.lbOutboundEndpoints) {
                    log.info(endpoint.getName() + " Avg RT : " + endpoint.getAvgResponseTime());
                }

                endPoint = lbOutboundEndpoints.get(0);
                this.index++;


            } else {

                log.error("No OutboundEndpoint is available..");

            }
        }

        return endPoint;
    }

    /**
     * Each implementation of LB algorithm will have certain values pertained to it.
     * (Eg: Round robin keeps track of index of OutboundEndpoint).
     * Implementation of this method will resetHealthPropertiesToDefault them.
     */
    @Override
    public void reset() {

    }

    /**
     * @return Object used for locking.
     */
    @Override
    public Object getLock() {

        return this.lock;
    }
}
