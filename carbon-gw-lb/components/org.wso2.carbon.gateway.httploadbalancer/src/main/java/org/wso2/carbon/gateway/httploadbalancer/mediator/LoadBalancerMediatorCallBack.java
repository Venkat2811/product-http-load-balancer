package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.Mediator;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * Callback related to LoadBalancerMediator.
 * TODO: In case of cookie persistence, appropriate cookie will be appended with response here.
 */
public class LoadBalancerMediatorCallBack implements CarbonCallback {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediatorCallBack.class);

    //Incoming callback.
    private CarbonCallback parentCallback;

    //LoadBalancer Mediator.
    private Mediator mediator;

    //LoadBalancerConfigContext context.
    private LoadBalancerConfigContext context;

    public LoadBalancerMediatorCallBack(CarbonCallback parentCallback,
                                        Mediator mediator, LoadBalancerConfigContext context) {

        this.parentCallback = parentCallback;
        this.mediator = mediator;
        this.context = context;

    }

    @Override
    public void done(CarbonMessage carbonMessage) {


        if (parentCallback instanceof LoadBalancerMediatorCallBack) {
            parentCallback.done(carbonMessage);
        } else if (mediator.hasNext()) { // If Mediator has a sibling after this
            try {
                mediator.next(carbonMessage, parentCallback);
            } catch (Exception e) {
                log.error("Error while mediating from Callback", e);
            }
        }

    }
}
