package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.GWConfigHolder;
import org.wso2.carbon.gateway.core.flow.Group;
import org.wso2.carbon.gateway.core.flow.Pipeline;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * LoadBalancerMediatorBuilder.
 * TODO: Resolve Group Issue -> There must be only one group in a config in case of LB.
 * TODO: If more than one group has to be supported, then OutboundEndpoints should be defined specific to group
 */
public class LoadBalancerMediatorBuilder {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediatorBuilder.class);

    private static LoadBalancerMediator lbMediator;

    public static LoadBalancerMediator configure(GWConfigHolder gwConfigHolder, LoadBalancerConfigContext context) {

        lbMediator = new LoadBalancerMediator(
                CommonUtil.getOutboundEndpointsList(gwConfigHolder.getOutboundEndpoints()), context);

        if (gwConfigHolder.hasGroups()) {

            List<Group> groups = new ArrayList<>(gwConfigHolder.getGroups());
            Pipeline[] pipelines = new Pipeline[groups.size()];
            for (int i = 0; i < groups.size(); i++) {
                pipelines[i] = gwConfigHolder.
                        getPipeline(gwConfigHolder.getGroup(groups.get(i).getPath()).getPipeline());
                pipelines[i].addMediator(lbMediator);
            }

        } else {

            gwConfigHolder.
                    getPipeline(gwConfigHolder.getInboundEndpoint().getPipeline()).addMediator(lbMediator);
        }


        return lbMediator;
    }
}
