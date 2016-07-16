package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.GWConfigHolder;

import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;


/**
 * LoadBalancerMediatorBuilder.
 */
public class LoadBalancerMediatorBuilder {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediatorBuilder.class);


    /**
     * @param gwConfigHolder GWConfigHolder.
     * @param context        LoadBalancerConfigContex.
     *                       <p>
     *                       This method is where LoadBalancerMediator is added to Pipeline.
     *                       <p>
     *                       Groups are also handled here.
     */
    public static void configure(GWConfigHolder gwConfigHolder, LoadBalancerConfigContext context) {


        LoadBalancerMediator lbMediator = new LoadBalancerMediator(
                CommonUtil.getLBOutboundEndpointsList(context.getLbOutboundEndpoints()), context,
                // We will be using this name for our timer. If configHolder has a name we will use that.
                // Otherwise we will use InboundEndpoint's name.
                // Anyways, it will be unique and will be easy to debug.
                (gwConfigHolder.getName() == null || gwConfigHolder.getName().equals("default")) ?
                        gwConfigHolder.getInboundEndpoint().getName() : gwConfigHolder.getName());

        gwConfigHolder.
                getPipeline(gwConfigHolder.getInboundEndpoint().getPipeline()).addMediator(lbMediator);

    }

    public static void configureForGroup(GWConfigHolder gwConfigHolder,
                                         LoadBalancerConfigContext context, String groupPath) {

        LoadBalancerMediator lbMediator = new LoadBalancerMediator(
                CommonUtil.getLBOutboundEndpointsList(context.getLbOutboundEndpoints()), context,
                // We will be using this name for our timer. If configHolder has a name we will use that.
                // Otherwise we will use InboundEndpoint's name.
                // Anyways, it will be unique and will be easy to debug.
                // Here we are appending groupPath also.
                ((gwConfigHolder.getName() == null || gwConfigHolder.getName().equals("default")) ?
                        gwConfigHolder.getInboundEndpoint().getName() : gwConfigHolder.getName()) + groupPath);
        gwConfigHolder.
                getPipeline(gwConfigHolder.getGroup(groupPath).getPipeline()).addMediator(lbMediator);

    }

}
