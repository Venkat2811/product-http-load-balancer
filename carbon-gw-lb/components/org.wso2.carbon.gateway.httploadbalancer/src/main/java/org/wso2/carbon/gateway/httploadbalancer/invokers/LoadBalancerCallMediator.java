package org.wso2.carbon.gateway.httploadbalancer.invokers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.ConfigRegistry;
import org.wso2.carbon.gateway.core.config.ParameterHolder;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorCallBack;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * CallMediator for LoadBalancer.
 */
public class LoadBalancerCallMediator extends AbstractMediator {

    private String outboundEPKey;

    private OutboundEndpoint outboundEndpoint;

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerCallMediator.class);

    public LoadBalancerCallMediator() {
        log.info("Inside LoadBalancerCallMediator..");
    }

    public LoadBalancerCallMediator(String outboundEPKey) {
        log.info("Inside LoadBalancerCallMediator.." + outboundEPKey);

        this.outboundEPKey = outboundEPKey;
    }

    public LoadBalancerCallMediator(OutboundEndpoint outboundEndpoint) {
        log.info("Inside LoadBalancerCallMediator.." + outboundEndpoint.getName());

        this.outboundEndpoint = outboundEndpoint;
    }

    public void setParameters(ParameterHolder parameterHolder) {
        outboundEPKey = parameterHolder.getParameter("endpointKey").getValue();
        log.info("Inside CallMediator setParameter().." + outboundEPKey);

    }

    @Override
    public String getName() {
        return "LoadBalancerCallMediator";
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback)
            throws Exception {

        log.info("Inside LoadBalancerCallMediator receive()..");

        OutboundEndpoint endpoint = outboundEndpoint;
        if (endpoint == null) {
            endpoint = ConfigRegistry.getInstance().getOutboundEndpoint(outboundEPKey);

            if (endpoint == null) {
                log.error("Outbound Endpoint : " + outboundEPKey + "not found ");
                return false;
            }
        }

        //Using separate LBMediatorCallBack because, we need to add special headers for session persistence...
        CarbonCallback callback = new LoadBalancerMediatorCallBack(carbonCallback, this);

        endpoint.receive(carbonMessage, callback);

        log.info("Inside end of LoadBalancerCallMediator receive()..");
        return false;
    }
}
