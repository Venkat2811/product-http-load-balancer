package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.timers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * TimeoutHandler for LoadBalancerMediatorCallBack.
 */
public class TimeoutHandler extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(TimeoutHandler.class);


    private final LoadBalancerConfigContext context;
    private final String handlerName;

    private volatile boolean isRunning = false;

    public TimeoutHandler(LoadBalancerConfigContext context, String configName) {

        this.context = context;
        this.handlerName = configName + "-" + this.getName();

        log.info(this.getHandlerName() + " started.");
    }

    public String getName() {

        return "TimeoutHandler";
    }

    public String getHandlerName() {
        return handlerName;
    }

    @Override
    public void run() {

        if (isRunning) {
            return;
        }

        //TODO: Do we need locking here..?
        processCallBackPool();

        isRunning = false;

    }

    private void processCallBackPool() {

        //As we are locking on callback pool it is efficient.
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

                    if (((currentTime - callBack.getTimeout()) > context.getReqTimeout())) {
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
                        }

                        /**
                         * But here we need synchronization because, this LBOutboundEndpoint might be
                         * used in CallMediator and LoadBalancerMediatorCallBack.
                         *
                         * We will be changing LBOutboundEndpoint's properties here.
                         */
                        synchronized (callBack.getLbOutboundEndpoint()) {

                            log.info("Work in progress");
                            //TODO: Since this is timedOut, we have to send appropriate message to client.

                            callBack.getLbOutboundEndpoint().incrementUnHealthyRetries();

                            if (this.reachedUnHealthyRetriesThreshold(callBack.getLbOutboundEndpoint())) {

                                callBack.getLbOutboundEndpoint().flipHealthyFlag();
                                callBack.getLbOutboundEndpoint().setHealthCheckedTime(this.getCurrentTime());

                                //Adding to unHealthy List.
                                synchronized (context.getUnHealthyLBEPList()) {

                                    context.getUnHealthyLBEPList().add(callBack.getLbOutboundEndpoint());
                                }

                            }
                        }

                    }
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
