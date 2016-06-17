package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.handlers.error.LBErrorHandler;
import org.wso2.carbon.messaging.DefaultCarbonMessage;

import java.util.concurrent.TimeUnit;

/**
 * TimeoutHandler for LoadBalancerMediatorCallBack.
 */
public class TimeoutHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TimeoutHandler.class);


    private final LoadBalancerConfigContext context;
    private final String handlerName;
    private final LoadBalancingAlgorithm algorithm;

    //To avoid race condition if any.
    private volatile boolean isRunning = false;

    public TimeoutHandler(LoadBalancerConfigContext context, LoadBalancingAlgorithm algorithm, String configName) {

        this.context = context;
        this.algorithm = algorithm;
        this.handlerName = configName + "-" + this.getName();

        log.info(this.getHandlerName() + " started.");
    }

    public String getName() {

        return "TimeoutHandler";
    }

    private String getHandlerName() {
        return handlerName;
    }

    @Override
    public void run() {

        if (isRunning) {
            return;
        }

        processCallBackPool();

        isRunning = false;

    }

    private void processCallBackPool() {

        boolean hasContent = false;

        // Only operations on Concurrent HashMap are thread safe.
        // Since we are retrieving it's size locking is better.
        // Lock is released after getting size.
        synchronized (context.getCallBackPool()) {

            if (context.getCallBackPool().size() > 0) {
                hasContent = true;
            }
        }

        //If there is no object in pool no need to process.
        if (hasContent) {


            long currentTime = this.getCurrentTime();

            /**
             * Here also we are not locking callBackPool, because lock on concurrent HashMap will be overkill.
             * At this point 2 cases might occur.
             *
             *  1) A new object will be added to the call back pool. We need not worry about it because,
             *     it will be added just now so it will not get timedOut.
             *
             *  2) A response arrives and corresponding object is removed from pool. So don't worry.
             *     We got response and it would have been sent to client.
             *
             *     NOTE: By iterating through Key Set we are getting callback object
             *     from our callBackPool.
             */

            for (String key : context.getCallBackPool().keySet()) {

                if (context.getCallBackPool().get(key) instanceof LoadBalancerMediatorCallBack) {


                    LoadBalancerMediatorCallBack callBack = (LoadBalancerMediatorCallBack)
                            context.getCallBackPool().get(key);

                    /**
                     * CallBack might be null because, we are iterating using keySet. So when getting keySet()
                     * we will get keys of all objects present in pool at that point.
                     *
                     * Suppose a response arrives after getting keySet(), that callBack will be removed from
                     * pool and it will also be reflected here because we are accessing callbackPool through
                     * context.getCallBackPool(), instead of having a local reference to it.
                     *
                     * So doing null check is better.
                     */
                    if (callBack != null) {

                        if (((currentTime - (callBack.getCreatedTime()
                                + LoadBalancerConstants.DEFAULT_GRACE_PERIOD)) > context.getReqTimeout())) {
                            //This callBack is in pool after it has timedOut.

                            //This operation is on Concurrent HashMap, so no synchronization is required.
                            if (!context.isInCallBackPool(callBack)) {
                                //If response arrives at this point, callBack would have been removed from pool
                                //and it would have been sent back to client. So break here.
                                break;
                            } else {
                                context.removeFromCallBackPool(callBack);
                                //From this point, this callback will not be available in pool.
                                //So if response arrives it will be discarded.


                                new LBErrorHandler().handleFault
                                        ("504", new Throwable("Gateway TimeOut"),
                                                new DefaultCarbonMessage(true), callBack);


                            }


                            if (!this.context.getHealthCheck().equals(LoadBalancerConstants.NO_HEALTH_CHECK)) {
                                /**
                                 * But here we need synchronization because, this LBOutboundEndpoint might be
                                 * used in CallMediator and LoadBalancerMediatorCallBack.
                                 *
                                 * We will be changing LBOutboundEndpoint's properties here.
                                 *
                                 * If an LBOutboundEndpoint is unHealthy it should not be available else where.
                                 * So we are locking on it, till we remove it from all the places
                                 * where it is available.
                                 *
                                 * NOTE: The below code does only HealthCheck related activities.
                                 *
                                 * Locking here is MUST because we want the below operations
                                 * to happen without any interference.
                                 */
                                synchronized (callBack.getLbOutboundEndpoint().getLock()) {

                                    callBack.getLbOutboundEndpoint().incrementUnHealthyRetries();
                                    /**
                                     * IMPORTANT: Here in case of LeastResponseTime algorithm,
                                     * we are doing send response time as 0,
                                     * other wise detection of unHealthyEndpoint will be late.
                                     */
                                    if (context.getAlgorithm().equals(LoadBalancerConstants.LEAST_RESPONSE_TIME)) {

                                        callBack.getLbOutboundEndpoint().
                                                computeAndSetAvgResponseTime(0);
                                    }

                                    if (this.reachedUnHealthyRetriesThreshold(callBack.getLbOutboundEndpoint())) {

                                        callBack.getLbOutboundEndpoint().markAsUnHealthy();

                                        /**
                                         * When request is received at LoadBalancerMediator,
                                         *  1) It checks for persistence
                                         *  2) It checks for algorithm
                                         *  3) It checks with unHealthyList
                                         *
                                         * So here we are removing unHealthy Endpoint in this order and finally
                                         * adding it to unHealthyEndpoint list.
                                         */

                                        //This case will only be true in case of CLIENT_IP_HASHING
                                        //as persistence policy.
                                        if (context.getStrictClientIPHashing() != null) {

                                            context.getStrictClientIPHashing().
                                                    removeLBOutboundEndpoint(callBack.getLbOutboundEndpoint());

                                        }

                                        //We are acquiring lock on Object that is available in algorithm.
                                        //We are removing the UnHealthyEndpoint from Algorithm List so that it
                                        //will not be chosen by algorithm.
                                        //Locking here is MUST because we want the below
                                        //operations to happen without any interference.
                                        synchronized (algorithm.getLock()) {

                                            algorithm.removeLBOutboundEndpoint(callBack.getLbOutboundEndpoint());
                                            algorithm.reset();

                                        }

                                        /**
                                         * Adding to unHealthy List if it is not already in least.
                                         * Synchronization is not necessary because, it is ConcurrentLinkedQueue.
                                         **/

                                        if (!context.getUnHealthyLBEPQueue().
                                                contains(callBack.getLbOutboundEndpoint())) {
                                            context.getUnHealthyLBEPQueue().add(callBack.getLbOutboundEndpoint());
                                        }


                                    }
                                }

                            }

                        }
                    }
                } else { // TODO: Remove this later.
                    log.info(key + " Object is in callBackPool..");
                }
            }
        }

    }

    private boolean reachedUnHealthyRetriesThreshold(LBOutboundEndpoint lbOutboundEndpoint) {

        if (lbOutboundEndpoint.getUnHealthyRetriesCount() >= context.getUnHealthyRetries()) {
            return true;
        } else {
            return false;
        }
    }


    private long getCurrentTime() {

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
