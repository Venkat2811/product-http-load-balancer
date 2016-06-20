package org.wso2.carbon.gateway.httploadbalancer.algorithm.weighted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.simple.LeastResponseTime;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of weighted Round Robin Algorithm.
 * <p>
 * User has to define weights for each endpoint. By default weight is 1.
 */
public class WeightedRoundRobin implements LoadBalancingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(WeightedRoundRobin.class);
    private final Object lock = new Object();

    private List<WeightedLBOutboundEndpoint> weightedLBOutboundEndpoints = new ArrayList<>();
    private Map<String, WeightedLBOutboundEndpoint> map = new ConcurrentHashMap<>();

    private int index = 0;
    private int WINDOW = 0;
    private int windowTracker

    /**
     * @return the name of implemented LB algorithm.
     */
    @Override
    public String getName() {
        return LoadBalancerConstants.WEIGHTED_ROUND_ROBIN;
    }

    /**
     * @param lbOutboundEPs list of all Outbound Endpoints to be load balanced.
     */
    @Override
    public void setLBOutboundEndpoints(List<LBOutboundEndpoint> lbOutboundEPs) {

    }

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be added to the existing list.
     */
    @Override
    public void addLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

    }

    /**
     * @param lbOutboundEndpoint outboundEndpoint to be removed from existing list.
     */
    @Override
    public void removeLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint) {

    }

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return the next LBOutboundEndpoint according to implemented LB algorithm.
     */
    @Override
    public LBOutboundEndpoint getNextLBOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        LBOutboundEndpoint endpoint = null;

        synchronized (this.lock) {
            if (this.weightedLBOutboundEndpoints != null && this.weightedLBOutboundEndpoints.size() > 0) {


                if (this.weightedLBOutboundEndpoints.size() > 1 && this.windowTracker > WINDOW) {

                    computeRatio();
                    this.windowTracker = 0;
                }

                // It is okay to do roundRobin for first few requests till it reaches WINDOW size.
                // After that it'll be proper LeastResponseTime based load distribution.

                LeastResponseTime.LBOutboundEPLeastRT outboundEPLeastRT = this.getNextEndpoint();

                endPoint = outboundEPLeastRT.getLbOutboundEndpoint();

                if (log.isDebugEnabled()) {
                    log.debug(outboundEPLeastRT.getName() + " RT : " + outboundEPLeastRT.getAvgResponseTime() +
                            " Curr : " + outboundEPLeastRT.getCurrentRequests() + " Max : "
                            + outboundEPLeastRT.getMaxRequestsPerWindow());
                }

                this.windowTracker++;

            } else {

                log.error("No OutboundEndpoint is available..");

            }
        }

        return endpoint;
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


    private class WeightedLBOutboundEndpoint {

        private LBOutboundEndpoint lbOutboundEndpoint;

        private int maxWeight = 1;
        private int currentWeight = 0;


        WeightedLBOutboundEndpoint(LBOutboundEndpoint lbOutboundEndpoint, int weight) {
            this.lbOutboundEndpoint = lbOutboundEndpoint;
            this.maxWeight = weight;
            map.put(this.lbOutboundEndpoint.getName(), this);
        }

        public String getName() {

            return this.lbOutboundEndpoint.getName();
        }

        private void incrementCurrentWeight() {
            this.currentWeight++;
        }

        public int getMaxWeight() {
            return maxWeight;
        }

        public int getCurrentWeight() {
            return currentWeight;
        }

        /**
         *
         * @param carbonMessage
         * @param carbonCallback
         * @param context
         * @return
         * @throws Exception
         *
         * NOTE: When this algorithm mode is chosen, all requests are sent through this method only.
         *       So currentWeight will be incremented in both the cases.
         *       (i.e.) In Endpoint chosen by persistence and in endpoint chosen by algorithm.
         */
        boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                        LoadBalancerConfigContext context) throws Exception {


            synchronized (lock) {
                this.incrementCurrentWeight();
            }
            this.lbOutboundEndpoint.receive(carbonMessage, carbonCallback, context);
            return false;
        }

        void resetWeights() {
            this.currentWeight = 0;
        }

        public Object getLock() {
            return this.lbOutboundEndpoint.getLock();
        }
    }
}
