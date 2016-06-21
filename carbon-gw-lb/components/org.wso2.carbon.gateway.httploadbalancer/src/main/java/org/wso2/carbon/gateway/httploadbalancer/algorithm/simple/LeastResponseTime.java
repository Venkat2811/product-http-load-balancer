package org.wso2.carbon.gateway.httploadbalancer.algorithm.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * Implementation of LeastResponseTime.
 * <p>
 * All Endpoints are assumed to have equal weights.
 */
public class LeastResponseTime implements LoadBalancingAlgorithm, Simple {

    private static final Logger log = LoggerFactory.getLogger(LeastResponseTime.class);
    private final Object lock = new Object();

    private List<LBOutboundEPLeastRT> lbOutboundEPLeastRTs = new ArrayList<>();

    private Map<String, LBOutboundEPLeastRT> map;

    private static final int WINDOW = 10;
    private int windowTracker = 0;
    private int index = 0;


    /**
     * Constructor.
     *
     * @param lbOutboundEndpoints
     */
    public LeastResponseTime(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        this.setLBOutboundEndpoints(lbOutboundEndpoints);
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
            map = new ConcurrentHashMap<>();
            for (LBOutboundEndpoint endpoint : lbOutboundEPs) {
                this.lbOutboundEPLeastRTs.add(new LBOutboundEPLeastRT(endpoint));
            }

        }
    }

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be added to the existing list.
     *                           <p>
     *                           This method will be used to add an endpoint once it
     *                           is back to healthy state.
     *                           <p>
     *                           Adding is different here.  We have to get it from map,
     *                           reset its properties and add it back to the list.
     */
    @Override
    public void addLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (this.lock) {
            if (map.containsKey(lbOutboundEndpoint.getName())) {

                if (this.lbOutboundEPLeastRTs.contains(map.get(lbOutboundEndpoint.getName()))) {
                    log.error(lbOutboundEndpoint.getName() + " already exists in list..");
                } else {
                    map.get(lbOutboundEndpoint.getName()).resetResponseTimeProperties(); //This is MUST.
                    this.lbOutboundEPLeastRTs.add(map.get(lbOutboundEndpoint.getName()));
                }

            } else {
                log.error("Cannot add a new endpoint like this. Use setLBOutboundEndpoints method" +
                        " or Constructor..");

            }
        }
    }

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be removed from existing list.
     *                           <p>
     *                           This method will be used to remove an unHealthyEndpoint.
     *                           <p>
     *                           NOTE: for this algorithm, we are not removing from map.
     *                           But, we are removing from list.
     *                           <p>
     *                           We are doing this because, for health check we need it.
     */
    @Override
    public void removeLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

        synchronized (this.lock) {
            if (map.containsKey(lbOutboundEndpoint.getName())) {

                if (this.lbOutboundEPLeastRTs.contains(map.get(lbOutboundEndpoint.getName()))) {

                    this.lbOutboundEPLeastRTs.remove(map.get(lbOutboundEndpoint.getName()));
                } else {
                    log.error(lbOutboundEndpoint.getName() + " has already been removed from list..");
                }

            } else {
                log.error(lbOutboundEndpoint.getName() + " is not in map..");
            }


        }
    }


    private void computeRatio() {

        int meanResponseTime = 0;

        for (LBOutboundEPLeastRT endPoint : this.lbOutboundEPLeastRTs) {

            synchronized (endPoint.getLock()) {
                meanResponseTime += endPoint.getAvgResponseTime();
            }
        }
        if (meanResponseTime % 2 == 0) {
            meanResponseTime = (meanResponseTime / this.lbOutboundEPLeastRTs.size());

        } else {
            meanResponseTime = ((meanResponseTime / this.lbOutboundEPLeastRTs.size()) + 1);

        }

        for (LBOutboundEPLeastRT endPoint : this.lbOutboundEPLeastRTs) {

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
     * For getting next LBOutboundEndpoint.
     * This method is called after locking, so don't worry.
     */
    private void incrementIndex() {

        this.index++;
        this.index %= this.lbOutboundEPLeastRTs.size();
    }

    public void incrementWindowTracker() {

        this.windowTracker++;

    }

    private LBOutboundEPLeastRT getNextEndpoint() {

        LBOutboundEPLeastRT endPoint = null;

        int counter = 0;

        while (true) {

            if (this.lbOutboundEPLeastRTs.get(this.index).getCurrentRequests() <
                    this.lbOutboundEPLeastRTs.get(this.index).getMaxRequestsPerWindow()) {

                endPoint = this.lbOutboundEPLeastRTs.get(this.index);
                break;
            } else {
                incrementIndex();
            }

            if (counter > lbOutboundEPLeastRTs.size()) {
                // This case will be useful if all endpoints have equal response time in a WINDOW.
                endPoint = this.lbOutboundEPLeastRTs.get(this.index);
                break;
            }
            counter++;
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
            if (this.lbOutboundEPLeastRTs != null && this.lbOutboundEPLeastRTs.size() > 0) {


                if (this.lbOutboundEPLeastRTs.size() > 1 && this.windowTracker >= WINDOW) {

                    computeRatio();
                    this.windowTracker = 0;
                }

                // It is okay to do roundRobin for first few requests till it reaches WINDOW size.
                // After that it'll be proper LeastResponseTime based load distribution.

                LBOutboundEPLeastRT outboundEPLeastRT = this.getNextEndpoint();

                endPoint = outboundEPLeastRT.getLbOutboundEndpoint();

                if (log.isDebugEnabled()) {
                    log.debug(outboundEPLeastRT.getName() + " RT : " + outboundEPLeastRT.getAvgResponseTime() +
                            " Curr : " + outboundEPLeastRT.getCurrentRequests() + " Max : "
                            + outboundEPLeastRT.getMaxRequestsPerWindow());
                }

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

            if (this.lbOutboundEPLeastRTs.size() > 0 && this.index >= this.lbOutboundEPLeastRTs.size()) {
                this.index %= this.lbOutboundEPLeastRTs.size();
            } else if (this.lbOutboundEPLeastRTs.size() == 0) {
                this.index = 0;
            }
        }

    }

    public void setAvgResponseTime(LBOutboundEndpoint lbOutboundEndpoint, int newTime) {

        map.get(lbOutboundEndpoint.getName()).computeAndSetAvgResponseTime(newTime);

    }

    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                           LoadBalancerConfigContext context,
                           LBOutboundEndpoint lbOutboundEndpoint) throws Exception {


        map.get(lbOutboundEndpoint.getName()).receive(carbonMessage, carbonCallback, context);
        return false;
    }

    /**
     * @return Object used for locking.
     */
    @Override
    public Object getLock() {

        return this.lock;
    }


    /**
     * We need few additional attributes for LeastResponseTime algorithm.
     * <p>
     * So, we are creating an inner class specially for this.
     */

    private class LBOutboundEPLeastRT {

        private LBOutboundEndpoint lbOutboundEndpoint;

        /**
         * These attributes are for LeastResponseTime Algorithm.
         */
        private int avgResponseTime = 0; // This stores running average.
        private int percentage = 100;
        private int maxRequestsPerWindow = WINDOW;
        private int currentRequests = 0; //This stores current no of requests in window.

        LBOutboundEPLeastRT(LBOutboundEndpoint lbOutboundEndpoint) {
            this.lbOutboundEndpoint = lbOutboundEndpoint;
            map.put(this.lbOutboundEndpoint.getName(), this);
        }

        public String getName() {

            return this.lbOutboundEndpoint.getName();
        }

        public LBOutboundEndpoint getLbOutboundEndpoint() {
            return this.lbOutboundEndpoint;
        }

        void setPercentage(int percentage) {
            this.percentage = percentage;
        }

        int getPercentage() {
            return this.percentage;
        }

        int getCurrentRequests() {
            return this.currentRequests;
        }

        void setCurrentRequests(int currentRequests) {
            this.currentRequests = currentRequests;
        }

        int getMaxRequestsPerWindow() {
            return maxRequestsPerWindow;
        }

        void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
            this.maxRequestsPerWindow = maxRequestsPerWindow;
        }

        private void incrementCurrentRequests() {
            this.currentRequests++;
        }

        /**
         * @param newTime Most resent response time of the endpoint.
         *                Calculates Running average of response time of that endpoint.
         */
        void computeAndSetAvgResponseTime(int newTime) {

            if (this.avgResponseTime != 0) { //For first time we should not divide by 2.

                if ((this.avgResponseTime + newTime) % 2 == 0) {
                    this.avgResponseTime = (this.avgResponseTime + newTime) / 2; // Dividing by 2.

                } else {
                    this.avgResponseTime = (((this.avgResponseTime + newTime) / 2) + 1);

                }

            } else {

                this.avgResponseTime = newTime;
            }
        }

        int getAvgResponseTime() {

            return this.avgResponseTime;

        }


        /**
         * @param carbonMessage
         * @param carbonCallback
         * @param context
         * @return
         * @throws Exception NOTE: When this algorithm mode is chosen, all requests are sent through this method only.
         *                   So currentRequests will be incremented in both the cases.
         *                   (i.e.) In Endpoint chosen by persistence and in endpoint chosen by algorithm.
         */
        boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                        LoadBalancerConfigContext context) throws Exception {


            synchronized (lock) {
                this.incrementCurrentRequests(); // Increments currentRequests for this LBOutboundEPLeastRT
                incrementWindowTracker(); // To keep track of no requests elapsed for this current window
            }
            this.lbOutboundEndpoint.receive(carbonMessage, carbonCallback, context);
            return false;
        }

        void resetResponseTimeProperties() {

            avgResponseTime = 0;
            percentage = 100;
            maxRequestsPerWindow = WINDOW;
            currentRequests = 0;

        }

        public Object getLock() {
            return this.lbOutboundEndpoint.getLock();
        }

    }
}
