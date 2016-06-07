package org.wso2.carbon.gateway.httploadbalancer.invokers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * CallMediator for LoadBalancer.
 */
public class LoadBalancerCallMediator extends AbstractMediator {


    private LBOutboundEndpoint lbOutboundEndpoint;

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerCallMediator.class);

    private final LoadBalancerConfigContext context;



    /**
     * @param lbOutboundEndpoint LBOutboundEndpoint.
     * @param context          LoadBalancerConfigContext.
     */
    public LoadBalancerCallMediator(LBOutboundEndpoint lbOutboundEndpoint,
                                    LoadBalancerConfigContext context) {


        this.lbOutboundEndpoint = lbOutboundEndpoint;
        this.context = context;
    }


    @Override
    public String getName() {
        return "LoadBalancerCallMediator";
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws Exception {

        //Using separate LBMediatorCallBack because, we are handling headers in CallBack for session persistence.
        CarbonCallback callback = new LoadBalancerMediatorCallBack(carbonCallback, this, context);

        lbOutboundEndpoint.receive(carbonMessage, callback);

        //As we are locking only on CallBackPool object, it is efficient.
        synchronized (this.context.getCallBackPool()) {
            //TODO: this is wrong.
            this.context.addToCallBackPool(callback.toString(), System.currentTimeMillis());
        }

        return false;
    }
}
