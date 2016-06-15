package org.wso2.carbon.gateway.httploadbalancer.callback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Constants;


/**
 * Health Check callBack for LB.
 */
public class LBHealthCheckCallBack implements CarbonCallback {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediatorCallBack.class);



    //LBOutboundEndpoint.
    //This will be used to locate specific lbOutboundEndpoint for healthChecking purposes.
    private final LBOutboundEndpoint lbOutboundEndpoint;



    private final LoadBalancerConfigContext context;

    public LBHealthCheckCallBack(LoadBalancerConfigContext context,
                                 LBOutboundEndpoint lbOutboundEndpoint) {


        this.lbOutboundEndpoint = lbOutboundEndpoint;
        this.context = context;
    }

    @Override
    public void done(CarbonMessage carbonMessage) {

        log.info("Message received at LBHCallBack done...");


        //Locking is not required as we are operating on ConcurrentHashMap.
        if (this.context.isInCallBackPool((CarbonCallback)
                carbonMessage.getProperty(Constants.CALL_BACK))) {

            this.context.removeFromCallBackPool((CarbonCallback)
                    carbonMessage.getProperty(Constants.CALL_BACK));
            //From this point, this callback will not be available in pool.

            /**
             * Locking is done within method itself.
             */
                this.lbOutboundEndpoint.incrementHealthyRetries();

        } else {
            log.error(" HealthCheck Response received after removing callback from pool.." +
                    "You might have to adjust timeOut value to avoid this from happening.");

        }
    }

}

