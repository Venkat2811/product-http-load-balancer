/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.gateway.httploadbalancer.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;


/**
 * An instance of this class has a reference to an OutboundEndpoint.
 * It also holds other LB HealthCheck related values pertained to it.
 * <p>
 * NOTE: Inside LB all OutboundEndpoints MUST be accessed via this Object only.
 */
public class LBOutboundEndpoint {

    private static final Logger log = LoggerFactory.getLogger(LBOutboundEndpoint.class);
    private final Object lock = new Object();


    // HTTP or HTTPS Endpoint.
    private OutboundEndpoint outboundEndpoint;

    // Healthy or not.
    private boolean isHealthy = true;

    // No of retries to be done to see whether it is alive again.
    private int healthyRetriesCount = 0;

    // No of retries to be done to mark an endpoint as unHealthy.
    private int unHealthyRetriesCount = 0;


    public LBOutboundEndpoint(OutboundEndpoint outboundEndpoint) {
        this.outboundEndpoint = outboundEndpoint;
        this.isHealthy = true;
        this.healthyRetriesCount = 0;
        this.unHealthyRetriesCount = 0;
    }


    public Object getLock() {
        return this.lock;
    }

    public OutboundEndpoint getOutboundEndpoint() {
        return outboundEndpoint;
    }

    public boolean isHealthy() {
        synchronized (lock) {
            return isHealthy;
        }
    }


    public int getHealthyRetriesCount() {
        synchronized (lock) {
            return healthyRetriesCount;
        }
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void setHealthyRetriesCount(int healthyRetriesCount) {

        this.healthyRetriesCount = healthyRetriesCount;
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public int getUnHealthyRetriesCount() {

        return unHealthyRetriesCount;

    }

    public String getName() {
        return this.outboundEndpoint.getName();
    }

    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback,
                           LoadBalancerConfigContext context) throws Exception {

        /**  log.info("Inside LBOutboundEndpoint...");

         log.info("Transport Headers...");
         log.info(healthCheckCMsg.getHeaders().toString());

         log.info("Properties...");
         log.info(healthCheckCMsg.getProperties().toString());
         **/

       // carbonMessage = CommonUtil.appendLBIP(carbonMessage, true);

        this.outboundEndpoint.receive(carbonMessage, carbonCallback);

        //No need to synchronize as we are operating on concurrent HashMap.
        context.addToCallBackPool((LoadBalancerMediatorCallBack) carbonCallback);

        return false;
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void incrementUnHealthyRetries() {

        this.unHealthyRetriesCount++;

        log.warn("Incremented UnHealthyRetries count for endPoint : " + this.getName());
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void incrementHealthyRetries() {

        this.healthyRetriesCount++;

        if (log.isDebugEnabled()) {
            log.info("Incremented HealthyRetries count for endPoint : " + this.getName());
        }
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void markAsUnHealthy() {

        isHealthy = false;

        log.warn(this.getName() + " is unHealthy");
    }

    /**
     * Call this method when unHealthy LBEndpoint becomes Healthy
     * and also when when we receive a successful response from backend
     * so that we can avoid any false alarms (i.e when unHealthy endpoint is
     * incremented due to some timeOut but the actual endpoint is actually healthy).
     * <p>
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void resetHealthPropertiesToDefault() {

        this.isHealthy = true;
        this.unHealthyRetriesCount = 0;
        this.healthyRetriesCount = 0;

    }



}
