package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * This handler is responsible for periodic checking of
 * UnHealthyLBOutboundEndpoint list to see if any endpoint is back to healthy state again.
 */
public class BackToHealthyHandler extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(BackToHealthyHandler.class);

    private final LoadBalancerConfigContext context;
    private final String handlerName;
    private final LoadBalancingAlgorithm algorithm;

    //To avoid race condition if any.
    private volatile boolean isRunning = false;

    public BackToHealthyHandler(LoadBalancerConfigContext context,
                                LoadBalancingAlgorithm algorithm, String configName) {

        this.context = context;
        this.algorithm = algorithm;
        this.handlerName = configName + "-" + this.getName();

        log.info(this.getHandlerName() + " started.");

    }

    public String getName() {

        return "BackToHealthyHandler";
    }

    public String getHandlerName() {
        return handlerName;
    }

    @Override
    public void run() {

        if (isRunning) {
            return;
        }

        processUnHealthyEndpointList();

        isRunning = false;

    }

    private void processUnHealthyEndpointList() {

        boolean hasContent = false;

        // Only operations on ConcurrentLinkedQueue are thread safe.
        // Since we are retrieving it's size locking is better.
        // Lock is released after getting size.
        synchronized (context.getUnHealthyLBEPQueue()) {

            if (context.getUnHealthyLBEPQueue().size() > 0) {
                hasContent = true;
            }
        }


        //Tf there is no content in list no need to process.
        if (hasContent) {

            long currentTime = this.getCurrentTime();


            /**
             * Here we will remove and endpoint from the list and do necessary processing.
             * If it is back to healthy, we will not add it to the list again.
             * Otherwise we will add it back to queue at the end.
             *
             * Since we are iterating through a for loop, once size limit is reached loop breaks.
             * So it will not lead to infinite circular loop.
             */

            for (int i = 0; i < context.getUnHealthyLBEPQueue().size(); i++) {

                LBOutboundEndpoint lbOutboundEndpoint = context.getUnHealthyLBEPQueue().poll();

                if ((currentTime - lbOutboundEndpoint.getHealthCheckedTime()) > context.getHealthycheckInterval()) {

                    //TODO: Think How will you make a call to endpoint to check its health..?
                    //TODO: otherwise, after time has elapsed add it back to list,
                    //TODO:Again if it fails we can add it back to unHealthy list.

                    if (reachedHealthyRetriesThreshold(lbOutboundEndpoint)) {

                        lbOutboundEndpoint.resetToDefault();

                    } else {

                        //Since HealthyRetriesThreshold is not reached, we are adding it back to queue.
                        //Adding it back to Queue.
                        context.getUnHealthyLBEPQueue().add(lbOutboundEndpoint);
                    }

                } else {

                    //Since time has not elapsed, we are not processing it.
                    //Adding it back to Queue.
                    context.getUnHealthyLBEPQueue().add(lbOutboundEndpoint);
                }
            }

        }

    }

    private boolean reachedHealthyRetriesThreshold(LBOutboundEndpoint lbOutboundEndpoint) {

        if (lbOutboundEndpoint.getHealthyRetriesCount() >= context.getHealthyRetries()) {
            return true;
        } else {
            return false;
        }

    }

    private long getCurrentTime() {

        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}
