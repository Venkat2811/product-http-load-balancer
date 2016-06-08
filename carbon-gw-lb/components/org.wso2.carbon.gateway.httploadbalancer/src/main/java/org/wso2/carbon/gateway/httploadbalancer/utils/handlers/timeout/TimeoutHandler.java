package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancingAlgorithm;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import java.util.TimerTask;

/**
 * TimeoutHandler for LoadBalancerMediatorCallBack.
 */
public class TimeoutHandler extends TimerTask {

    private static final Logger log = LoggerFactory.getLogger(TimeoutHandler.class);


    private final LoadBalancingAlgorithm algorithm;
    private final LoadBalancerConfigContext context;

    private boolean isRunning = false;
    private final Object lock = new Object();

    public TimeoutHandler(LoadBalancingAlgorithm algorithm, LoadBalancerConfigContext context) {

        this.algorithm = algorithm;
        this.context = context;

    }


    @Override
    public void run() {

        if (isRunning) {
            return;
        }

        synchronized (lock) {

            isRunning = true;

            try {

            } finally {

                isRunning = false;
            }
        }

    }
}
