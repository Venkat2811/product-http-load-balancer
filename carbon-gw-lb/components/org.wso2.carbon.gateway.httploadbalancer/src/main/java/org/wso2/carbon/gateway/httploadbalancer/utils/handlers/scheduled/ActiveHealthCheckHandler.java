package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
import org.wso2.carbon.gateway.httploadbalancer.utils.exception.LBException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * This handler is responsible for periodically checking whether OutboundEndpoints are healthy or not.
 * <p>
 * This is Active Health Checking and it can detect unHealthy endpoints before failure occurs.
 * <p>
 * Tries to establish socket connection to OutboundEndpoints.
 */
public class ActiveHealthCheckHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ActiveHealthCheckHandler.class);

    private final LoadBalancerConfigContext context;
    private final String handlerName;
    private final LoadBalancingAlgorithm algorithm;
    //To avoid race condition if any.
    private volatile boolean isRunning = false;

    public ActiveHealthCheckHandler(LoadBalancerConfigContext context, LoadBalancingAlgorithm algorithm,
                                    String configName) {

        this.context = context;
        this.algorithm = algorithm;
        this.handlerName = configName + "-" + this.getName();

        log.info(this.getHandlerName() + " started.");

    }

    public String getName() {

        return "ActiveHealthCheckHandler";
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


        if (context.getLbOutboundEndpoints().size() > 0) {
            hasContent = true;
        }


        //Tf there are no LBOutboundEndpoints, no need to process.
        if (hasContent) {


            List<LBOutboundEndpoint> list = new ArrayList<>(context.getLbOutboundEndpoints().values());

            for (LBOutboundEndpoint lbOutboundEndpoint : list) {

                //If it is in UnHealthyQueue, we need not establish connection and check.
                if (context.getUnHealthyLBEPQueue().contains(lbOutboundEndpoint)) {
                    continue;
                }

                Socket connectionSock = null;

                while (true) {
                    if (connectionSock != null && connectionSock.isConnected()) {
                        try {
                            connectionSock.close();
                        } catch (IOException e) {
                            log.error(e.toString());
                        }
                    }

                    connectionSock = new Socket();
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

                                log.info(lbOutboundEndpoint.getName() + " is healthy..");
                                break;

                            } else {

                                throw new LBException("Connection timedOut for Endpoint : "
                                        + lbOutboundEndpoint.getName());
                            }

                        } else {
                            throw new LBException("Port value retrieved is -1");

                        }
                    } catch (IOException | InterruptedException | LBException e) {

                        log.error(e.toString());

                        synchronized (lbOutboundEndpoint.getLock()) {
                            lbOutboundEndpoint.incrementUnHealthyRetries();
                        }

                        if (reachedUnHealthyRetriesThreshold(lbOutboundEndpoint)) {

                            CommonUtil.removeUnHealthyEndpoint(context, algorithm, lbOutboundEndpoint);

                        } else {
                            log.info("No of unHealthy retries not yet reached...");
                            continue;
                        }
                        //This break is MUST.
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

        }

    }


    private boolean reachedUnHealthyRetriesThreshold(LBOutboundEndpoint lbOutboundEndpoint) {

        if (lbOutboundEndpoint.getUnHealthyRetriesCount() >= context.getUnHealthyRetries()) {
            return true;
        } else {
            return false;
        }

    }
}
