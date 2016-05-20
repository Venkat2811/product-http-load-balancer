package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.gateway.core.flow.Mediator;
import org.wso2.carbon.gateway.core.flow.MediatorProvider;


/**
 * Load balancer mediator provider.
 */
@Component(
        name = "LoadBalancerMediatorProvider",
        immediate = true,
        service = MediatorProvider.class
)
public class LoadBalancerMediatorProvider implements MediatorProvider {

    @Activate
    protected void start(BundleContext bundleContext) {
    }

    @Override
    public String getName() {
        return "LoadBalancerMediator";
    }

    @Override
    public Mediator getMediator() {
        return new LoadBalancerMediator();
    }


}
