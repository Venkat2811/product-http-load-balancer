package org.wso2.carbon.gateway.httploadbalancer.mediator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.GWConfigHolder;
import org.wso2.carbon.gateway.core.flow.AbstractMediator;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * LoadBalancerMediator.
 * TODO: To be implemented.
 */
public class LoadBalancerMediator extends AbstractMediator{

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerMediator.class);


    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        return false;
    }
}
