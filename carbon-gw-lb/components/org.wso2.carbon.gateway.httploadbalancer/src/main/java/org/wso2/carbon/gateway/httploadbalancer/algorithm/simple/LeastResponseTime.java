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
 * Implementation of LeastResponseTime.
 * <p>
 * All Endpoints are assumed to have equal weights.
 */
public class LeastResponseTime implements LoadBalancingAlgorithm {


/*
     * =====================================IMPLEMENTATION LOGIC==================================================== *
     *                                                                                                               *
     * FACT : If there is more load on a server, response time will be more & also a newly started server will       *
     *        have high response time initially because of its warm-up time. So till initial WINDOW no of requests   *
     *        it'll be ROUND-ROBIN.                                                                                  *
     *                                                                                                               *
     * EXPLANATION: See the below example to understand how this algorithm works. Below are the factors that         *
     *              influence our decision.                                                                          *
     *                                                                                                               *
     *              1) Average Response Time - This is calculated as "Running-Average" (i.e) at any point            *
     *                                         average-response time attribute of an LBOutbound endpoint gives       *
     *                                         the average of all its response time                                  *
     *                                                                                                               *
     *              2) WINDOW - This attribute defined below determines the number of requests after which           *
     *                          we should perform computation to find out load distribution.                         *
     *                                                                                                               *
     *              3) Max Request Per Window - This determines the maximum number of requests that can              *
     *                                          be sent to this endpoint in current WINDOW period.                   *
     *                                                                                                               *
     *                                                                                                               *
     *    NOTE: You should also note that, when this algorithm mode is selected, averageResponseTime and             *
     *          windowTracker will be computed for each and every requests whatever may be the persistence           *
     *          policy.                                                                                              *
     *                                                                                                               *
     *    EXAMPLE: Assume that we are having 4 endpoints A,B,C,D with their averageResponseTime (running average)    *
     *             2,2,8,8 (milli seconds) respectively.  This running average value is after WINDOW number of       *
     *             requests are being processed.                                                                     *
     *                                                                                                               *
     *             The below calculations will be performed once if (windowTracker > WINDOW).  windowTracker will be *
     *             reset to 0 each and every time it satisfies this condition.                                       *
     *                                                                                                               *
     *    CALCULATION:                                                                                               *
     *                 Now, (2+2+8+8)/4 = 5                                                                          *
     *                 So, the ideal average response time has to be 5, if we want to distribute load evenly.        *
     *                                                                                                               *
     *                 Now, 2/5 = 0.4                                                                                *
     *                      0.4*100 = 40% i.e., 100-40 = 60% which is ideal percentage of load to be handled         *
     *                                                   by this endpoint. ( For A & B )                             *
     *                                                                                                               *
     *                 Again, 8/5 = 1.6                                                                              *
     *                        1.6*100 = 160% i.e., 100-160 = -60% which is ideal percentage of load to be handled    *
     *                                                       by this endpoint. ( For C & D )                         *
     *                                                                                                               *
     *                 This is because, load is proportional to response time.                                       *
     *                 (i.e) more load will result in more response time.                                            *
     *                                                                                                               *
     *                 NOTE: Incaseof negative % we mark maxRequestsPerWindow = 1.                                   *
     *                       Also, endpoints chosen based on persistence policy will not care about this window max. *
     *                       Endpoints will be chosen based on persistence.                                          *
     *                                                                                                               *
     *                       Otherwise,   maxRequestsPerWindow = (percentage * WINDOW)/100                           *
     *                       Kindly note that this is maxReq.                                                        *
     *                                                                                                               *
     *                       So when any new requests without any persistence policy arrives,                        *
     *                       this algorithm will choose endpoint that have not exceeded this                         *
     *                       maxReqPerWindow in ROUND-ROBIN manner.  Thus load distribution                          *
     *                       is done.                                                                                *
     *                                                                                                               *
     *   By doing this we will be balancing load and thus bring down its response time gradually.                    *
     *                                                                                                               *
     *                                                                                                               *
     *                                                                                                               *
     *===============================================================================================================*
 */

    private static final Logger log = LoggerFactory.getLogger(LeastResponseTime.class);
    private final Object lock = new Object();

    private List<LBOutboundEndpoint> lbOutboundEndpoints;

    private static final int WINDOW = 10;
    private int windowTracker = 0;
    private int index = 0;


    /**
     * Constructor.
     *
     * @param lbOutboundEndpoints
     */
    public LeastResponseTime(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        this.lbOutboundEndpoints = lbOutboundEndpoints;


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

    private void computeRatio() {

        int meanResponseTime = 0;

        for (LBOutboundEndpoint endPoint : this.lbOutboundEndpoints) {

            synchronized (endPoint.getLock()) {
                meanResponseTime += endPoint.getAvgResponseTime();
            }
        }
        if (meanResponseTime % 2 == 0) {
            meanResponseTime = (meanResponseTime / this.lbOutboundEndpoints.size());

        } else {
            meanResponseTime = ((meanResponseTime / this.lbOutboundEndpoints.size()) + 1);

        }

        for (LBOutboundEndpoint endPoint : this.lbOutboundEndpoints) {

            synchronized (endPoint.getLock()) {
                endPoint.setPercentage((100 - ((endPoint.getAvgResponseTime() / meanResponseTime) * 100)));

                if (endPoint.getPercentage() > 0) {
                    endPoint.setMaxRequestsPerWindow(((endPoint.getPercentage() * WINDOW) / 100));
                } else {
                    endPoint.setMaxRequestsPerWindow(1);
                }

                endPoint.setCurrentRequests(0); //Resetting is MUST.

                if (log.isDebugEnabled()) {
                    log.debug(endPoint.getName() + " RT : " + endPoint.getAvgResponseTime() +
                            " Curr : " + endPoint.getCurrentRequests() + " Max : "
                            + endPoint.getMaxRequestsPerWindow());
                }
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

    public void incrementWindowTracker() {

        synchronized (this.lock) {
            this.windowTracker++;
        }
    }

    private LBOutboundEndpoint getNextEndpoint() {

        LBOutboundEndpoint endPoint = null;
        if (this.lbOutboundEndpoints.get(this.index).getCurrentRequests() <
                this.lbOutboundEndpoints.get(this.index).getMaxRequestsPerWindow()) {

            endPoint = this.lbOutboundEndpoints.get(this.index);
        } else {
            incrementIndex();
            endPoint = this.lbOutboundEndpoints.get(this.index);
        }

        incrementIndex();

        return endPoint;
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


                if (this.lbOutboundEndpoints.size() > 1 && this.windowTracker > WINDOW) {

                    computeRatio();
                    this.windowTracker = 0;
                }

                // It is okay to do roundRobin for first few requests till it reaches WINDOW size.
                // After that it'll be proper LeastResponseTime based load distribution.

                endPoint = this.getNextEndpoint();
                if (log.isDebugEnabled()) {
                    log.debug(endPoint.getName() + " RT : " + endPoint.getAvgResponseTime() +
                            " Curr : " + endPoint.getCurrentRequests() + " Max : "
                            + endPoint.getMaxRequestsPerWindow());
                }

                this.windowTracker++;

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

        synchronized (this.lock) {

            if (this.lbOutboundEndpoints.size() > 0 && this.index >= this.lbOutboundEndpoints.size()) {
                this.index %= this.lbOutboundEndpoints.size();
            } else {
                this.index = 0;
            }
        }

    }

    /**
     * @return Object used for locking.
     */
    @Override
    public Object getLock() {

        return this.lock;
    }
}
