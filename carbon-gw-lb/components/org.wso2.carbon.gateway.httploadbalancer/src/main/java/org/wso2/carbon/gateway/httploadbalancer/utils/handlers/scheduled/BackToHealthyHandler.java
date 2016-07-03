package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;


import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;


/**
 * This handler is responsible for periodic checking of
 * UnHealthyLBOutboundEndpoint list to see if any endpoint is back to healthy state again.
 * <p>
 * Tries to establish Socket Connection to unHealthyEndpoints.
 */

public class BackToHealthyHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(BackToHealthyHandler.class);

    private final LoadBalancerConfigContext context;
    private final String handlerName;
    private final LoadBalancingAlgorithm algorithm;
    //To avoid race condition if any.
    private volatile boolean isRunning = false;

    public BackToHealthyHandler(LoadBalancerConfigContext context, LoadBalancingAlgorithm algorithm,
                                 String configName) {

        this.context = context;
        this.algorithm = algorithm;
        this.handlerName = configName + "-" + this.getName();

        log.info(this.getHandlerName() + " started.");

    }

    public String getName() {

        return "BackToHealthyHandler";
    }

    private String getHandlerName() {
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

            for (LBOutboundEndpoint lbOutboundEndpoint : list) {

                while (true) {

                    Socket connectionSock = new Socket();
                    try {

                        InetAddress inetAddr = InetAddress.getByName(CommonUtil.
                                getHost(lbOutboundEndpoint.getOutboundEndpoint().getUri()));
                        int port = CommonUtil.getPort(lbOutboundEndpoint.getOutboundEndpoint().getUri());

                        if (port != -1) {

                            SocketAddress socketAddr = new InetSocketAddress(inetAddr, port);


                            connectionSock.connect(socketAddr, context.getReqTimeout());
                            //Waiting till timeOut..
                            Thread.sleep(context.getReqTimeout());

                            if (connectionSock.isConnected()) {
                                lbOutboundEndpoint.incrementHealthyRetries();

                                if (reachedHealthyRetriesThreshold(lbOutboundEndpoint)) {

                                    lbOutboundEndpoint.resetHealthPropertiesToDefault(); //Endpoint is back to healthy.
                                    log.info(lbOutboundEndpoint.getName() + " is back to healthy..");

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

                                        context.getStrictClientIPHashing().addLBOutboundEndpoint(lbOutboundEndpoint);
                                    }

                                    //We are acquiring lock on Object that is available in algorithm.
                                    //We are removing the UnHealthyEndpoint from Algorithm List so that it
                                    //will not be chosen by algorithm.
                                    //Locking here is MUST because we want the below
                                    //operations to happen without any interference.
                                    synchronized (algorithm.getLock()) {

                                        algorithm.addLBOutboundEndpoint(lbOutboundEndpoint);
                                        algorithm.reset();

                                    }

                                    /**
                                     * IMPORTANT: Removing endpoint from unHealthy Queue.
                                     */
                                    context.getUnHealthyLBEPQueue().remove(lbOutboundEndpoint);

                                } else {
                                    log.info("No of retries not yet reached...");
                                    continue;
                                }
                                //This break is MUST.
                                break;

                            } else {
                                proceessBeforeBreak("Connection timedOut for UnHealthy Endpoint : "
                                        + lbOutboundEndpoint.getName(), lbOutboundEndpoint);
                            }

                        } else {
                            proceessBeforeBreak("Port value retrieved is -1", lbOutboundEndpoint);
                        }
                    } catch (InterruptedException e) {
                        proceessBeforeBreak(e.toString(), lbOutboundEndpoint);
                        break;
                    } catch (UnknownHostException e) {
                        proceessBeforeBreak(e.toString(), lbOutboundEndpoint);
                        break;
                    } catch (IOException e) {
                        proceessBeforeBreak(e.toString(), lbOutboundEndpoint);
                        break;
                    } finally {
                        if (connectionSock.isConnected()) {
                            try {
                                connectionSock.close();
                            } catch (IOException e) {
                                log.error(e.toString());

                            }
                        }
                    }
                }
            }
            if (context.getUnHealthyLBEPQueue().size() == 0) {

                log.info("All endpoints are back to healthy state.");

            } else {

                log.warn("There are " + context.getUnHealthyLBEPQueue().size() + " unHealthy endpoint(s).");
            }

        }

    }

    private void proceessBeforeBreak(String error, LBOutboundEndpoint lbOutboundEndpoint) {
        log.error(error);
        lbOutboundEndpoint.setHealthyRetriesCount(0);
        log.warn(lbOutboundEndpoint.getName() + " is still unHealthy..");
    }

    private boolean reachedHealthyRetriesThreshold(LBOutboundEndpoint lbOutboundEndpoint) {

        if (lbOutboundEndpoint.getHealthyRetriesCount() >= context.getHealthyRetries()) {
            return true;
        } else {
            return false;
        }

    }
}
