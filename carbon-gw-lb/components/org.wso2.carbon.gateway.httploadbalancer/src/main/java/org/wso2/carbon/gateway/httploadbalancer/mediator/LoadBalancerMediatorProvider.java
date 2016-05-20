package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.wso2.carbon.gateway.core.flow.Mediator;
import org.wso2.carbon.gateway.core.flow.MediatorProvider;


/**
 * Load balancer mediator provider.
 */
public class LoadBalancerMediatorProvider implements MediatorProvider {

    @Override
    public String getName() {
        return "LoadBalancerMediator";
    }

    @Override
    public Mediator getMediator() {
        return new LoadBalancerMediator();
    }


}
