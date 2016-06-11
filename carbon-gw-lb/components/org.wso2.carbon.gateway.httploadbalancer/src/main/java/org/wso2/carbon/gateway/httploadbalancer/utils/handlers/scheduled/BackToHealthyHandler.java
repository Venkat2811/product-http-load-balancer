package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.callback.LBHealthCheckCallBack;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.invokers.LoadBalancerCallMediator;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.DefaultCarbonMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private Map<String, LoadBalancerCallMediator> lbCallMediatorMap;

    //To avoid race condition if any.
    private volatile boolean isRunning = false;

    public BackToHealthyHandler(LoadBalancerConfigContext context, LoadBalancingAlgorithm algorithm,
                                Map<String, LoadBalancerCallMediator> lbCallMediatorMap, String configName) {

        this.context = context;
        this.algorithm = algorithm;
        this.lbCallMediatorMap = lbCallMediatorMap;
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

            /**
             * Here we will remove and endpoint from the list and do necessary processing.
             * If it is back to healthy, we will not add it to the list again.
             * Otherwise we will add it back to queue at the end.
             *
             * Since we are iterating through a for loop, once size limit is reached loop breaks.
             * So it will not lead to infinite circular loop.
             */

            List<LBOutboundEndpoint> list = new ArrayList<>(context.getUnHealthyLBEPQueue());

            for (int i = 0; i < list.size(); i++) {

                LBOutboundEndpoint lbOutboundEndpoint = list.get(i);

                //TODO: Think How will you make a call to endpoint to check its health..?
                //TODO: otherwise, after time has elapsed add it back to list,
                //TODO:Again if it fails we can add it back to unHealthy list.


                while (true) {


                    DefaultCarbonMessage carbonMessage = new DefaultCarbonMessage();
                    //TODO: construct carbonMessage.

                    LBHealthCheckCallBack callBack = new LBHealthCheckCallBack(context, lbOutboundEndpoint);

                    try {
                        this.lbCallMediatorMap.get(lbOutboundEndpoint.getName()).receive(
                                carbonMessage,
                                callBack);
                    } catch (Exception e) {

                        log.error(e.toString());
                    }

                    try {
                        //Waiting for response to come.
                        Thread.sleep(context.getReqTimeout() + 10);

                    } catch (InterruptedException e) {
                        log.error(e.toString());
                    }

                    //If response has arrived it will not be in pool.
                    if (context.isInCallBackPool(callBack)) {

                        context.removeFromCallBackPool(callBack);

                        //Adding back to queue since it is unHealthy.
                        lbOutboundEndpoint.setHealthCheckedTime(this.getCurrentTime());
                        lbOutboundEndpoint.setHealthyRetriesCount(0);


                        log.warn(lbOutboundEndpoint.getName() + " is still unHealthy..");
                        break;

                    } else { //Ok, now response has arrived. We need to check for healthy retries.

                        if (reachedHealthyRetriesThreshold(lbOutboundEndpoint)) {

                            //TODO: If it is healthy, remove it from Queue.
                            lbOutboundEndpoint.resetToDefault(); //Endpoint is back to healthy.
                            log.info("Endpoint is back to healthy..");

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

                                synchronized (context.getStrictClientIPHashing()) {

                                    context.getStrictClientIPHashing().addLBOutboundEndpoint(lbOutboundEndpoint);

                                }
                            }

                            //We are acquiring lock on Object that is available in algorithm.
                            //We are removing the UnHealthyEndpoint from Algorithm List so that it
                            //will not be chosen by algorithm.
                            synchronized (algorithm.getLock()) {

                                algorithm.addLBOutboundEndpoint(lbOutboundEndpoint);
                                algorithm.reset();

                            }

                        } else {
                            log.info("To be implemented..");
                            break;
                        }

                    }


                }


            }
            log.info("Size : " + context.getUnHealthyLBEPQueue().size());

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
