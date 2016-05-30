package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.Mediator;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Constants;


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


        /**
         log.info("Inside LB mediator call back...");
         Map<String, String> transHeaders = carbonMessage.getHeaders();
         log.info("Transport Headers...");
         log.info(transHeaders.toString() + "\n\n");

         Map<String, Object> prop = carbonMessage.getProperties();
         log.info("Properties...");
         log.info(prop.toString());
         **/

        if (parentCallback instanceof LoadBalancerMediatorCallBack) {

            if (context.getPersistence().equals(LoadBalancerConstants.APPLICATION_COOKIE) ||
                    context.getPersistence().equals(LoadBalancerConstants.LB_COOKIE)) {

                String host = carbonMessage.getProperty(Constants.HOST).toString();
                String port = carbonMessage.getProperty(Constants.PORT).toString();


                String cookie = context.getCookieFromOutboundEP(
                        host + ":" + port);

                //TODO: update cookie of carbon message..
                log.info("Cookie to be inserted is : " + cookie);

                parentCallback.done(carbonMessage);

            } else {

                parentCallback.done(carbonMessage);
            }
        } else if (mediator.hasNext()) { // If Mediator has a sibling after this
            try {
                mediator.next(carbonMessage, parentCallback);
            } catch (Exception e) {
                log.error("Error while mediating from Callback", e);
            }
        }

    }
}
