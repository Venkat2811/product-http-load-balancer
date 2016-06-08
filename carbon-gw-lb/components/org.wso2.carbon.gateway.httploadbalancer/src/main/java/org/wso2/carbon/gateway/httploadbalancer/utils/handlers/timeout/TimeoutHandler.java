package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * TimeoutHandler for LoadBalancerMediatorCallBack.
 */
public class TimeoutHandler extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(TimeoutHandler.class);


    private final LoadBalancerConfigContext context;

    private volatile boolean isRunning = false;

    public TimeoutHandler(LoadBalancerConfigContext context) {

        this.context = context;
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

            /**
             * Here also we are not locking callBackPool, because lock on concurrent HashMap will be overkill.
             * At this point 2 cases might occur.
             *
             *  1) A new object will be added to the call back pool. We need not worry about it because,
             *     it will be added just now so it will not get timedOut.
             *
             *  2) A response arrives and corresponding object is removed from pool. So don't worry.
             *     We got response and it would have been sent to client.
             *     Note that by iterating through Key Set we are getting callback object
             *     from our callBackPool.
             */

            long currentTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

            for (String key : context.getCallBackPool().keySet()) {

                LoadBalancerMediatorCallBack callBack = (LoadBalancerMediatorCallBack)
                        context.getCallBackPool().get(key);

                if ((callBack.getTimeout() + context.getReqTimeout()) > currentTime) {
                    //There is an object in pool after request has timedOut.

                    context.removeFromCallBackPool(callBack);
                    //TODO: Since this is timedOut, we have to send appropriate message to client.
                }
            }
        }

    }
}
