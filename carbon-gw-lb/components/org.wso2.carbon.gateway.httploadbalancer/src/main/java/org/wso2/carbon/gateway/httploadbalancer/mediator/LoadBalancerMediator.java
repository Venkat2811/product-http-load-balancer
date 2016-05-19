package org.wso2.carbon.gateway.httploadbalancer.mediator;


import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * LoadBalancerMediator.
 * TODO: To be implemented.
 */
public class LoadBalancerMediator extends AbstractMediator {


    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        return false;
    }
}
