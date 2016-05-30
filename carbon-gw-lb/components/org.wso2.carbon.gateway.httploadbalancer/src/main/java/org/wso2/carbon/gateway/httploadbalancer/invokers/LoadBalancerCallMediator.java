package org.wso2.carbon.gateway.httploadbalancer.invokers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.ConfigRegistry;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorCallBack;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.Map;


/**
 * CallMediator for LoadBalancer.
 */
public class LoadBalancerCallMediator extends AbstractMediator {

    private String outboundEPKey;

    private OutboundEndpoint outboundEndpoint;

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerCallMediator.class);

    private LoadBalancerConfigContext context;

    public LoadBalancerCallMediator() {
        log.info("Inside LoadBalancerCallMediator..");
    }

    public LoadBalancerCallMediator(String outboundEPKey) {


        this.outboundEPKey = outboundEPKey;
    }

    public LoadBalancerCallMediator(OutboundEndpoint outboundEndpoint,
                                    LoadBalancerConfigContext context) {


        this.outboundEndpoint = outboundEndpoint;
        this.context = context;
    }


    @Override
    public String getName() {
        return "LoadBalancerCallMediator";
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws Exception {


        OutboundEndpoint endpoint = outboundEndpoint;
        if (endpoint == null) {
            endpoint = ConfigRegistry.getInstance().getOutboundEndpoint(outboundEPKey);

            if (endpoint == null) {
                log.error("Outbound Endpoint : " + outboundEPKey + "not found ");
                return false;
            }
        }


         log.info("Inside LB call mediator...");
         Map<String, String> transHeaders = carbonMessage.getHeaders();
         log.info("Transport Headers...");
         log.info(transHeaders.toString() + "\n\n");

         Map<String, Object> prop = carbonMessage.getProperties();
         log.info("Properties...");
         log.info(prop.toString() + "\n\n");


        //Using separate LBMediatorCallBack because, we need to add special headers for session persistence...
        CarbonCallback callback = new LoadBalancerMediatorCallBack(carbonCallback, this, context);

        endpoint.receive(carbonMessage, callback);


        return false;
    }
}
