package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.wso2.carbon.gateway.core.config.GWConfigHolder;
import org.wso2.carbon.gateway.core.config.ParameterHolder;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.utils.MapToListConverter;

/**
 * LoadBalancerMediatorBuilder.
 */
public class LoadBalancerMediatorBuilder {

    private static LoadBalancerMediator lbMediator;

    public static LoadBalancerMediator configure(GWConfigHolder gwConfigHolder, ParameterHolder parameterHolder) {

        lbMediator = new LoadBalancerMediator(
                MapToListConverter.getOutboundEndpointsList(gwConfigHolder.getOutboundEndpoints()),
                parameterHolder.getParameter(LoadBalancerConstants.ALGORITHM_NAME).getValue());

        gwConfigHolder.getPipeline(gwConfigHolder.getInboundEndpoint().getPipeline()).addMediator(lbMediator);


        return lbMediator;
    }
}
