package org.wso2.carbon.gateway.httploadbalancer.outbound;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;

/**
 * An instance of this class has a reference to an OutboundEndpoint.
 * It also holds other LB HealthCheck related values pertained to it.
 * <p>
 * NOTE: Inside LB all OutboundEndpoints MUST be accessed via this Object only.
 */
public class LBOutboundEndpoint {

    // HTTP or HTTPS Endpoint.
    private OutboundEndpoint outboundEndpoint;

    // Healthy or not.
    private boolean isHealthy = true;

    // No of retries to be done to see whether it is alive again.
    private int healthyRetriesCount = 0;

    // No of retries to be done to mark an endpoint as unHealthy.
    private int unHealthyRetriesCount = 0;

    // This will be used to hold System.currentTimeMillis() value.
    // This will be set, once OutboundEndpoint becomes unhealthy.
    // It will be set back to zero once endpoint becomes healthy again.
    private long healthCheckedTime = 0;

    public LBOutboundEndpoint(OutboundEndpoint outboundEndpoint) {
        this.outboundEndpoint = outboundEndpoint;
        this.isHealthy = true;
        this.healthyRetriesCount = 0;
        this.unHealthyRetriesCount = 0;
        this.healthCheckedTime = 0;
    }

    public OutboundEndpoint getOutboundEndpoint() {
        return outboundEndpoint;
    }

    public void setOutboundEndpoint(OutboundEndpoint outboundEndpoint) {
        this.outboundEndpoint = outboundEndpoint;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public int getHealthyRetriesCount() {
        return healthyRetriesCount;
    }

    public void setHealthyRetriesCount(int healthyRetriesCount) {
        this.healthyRetriesCount = healthyRetriesCount;
    }

    public int getUnHealthyRetriesCount() {
        return unHealthyRetriesCount;
    }

    public void setUnHealthyRetriesCount(int unHealthyRetriesCount) {
        this.unHealthyRetriesCount = unHealthyRetriesCount;
    }

    public long getHealthCheckedTime() {
        return healthCheckedTime;
    }

    public void setHealthCheckedTime(long healthCheckedTime) {
        this.healthCheckedTime = healthCheckedTime;
    }

    public String getName() {
        return this.outboundEndpoint.getName();
    }

    public boolean receive(CarbonMessage carbonMessage, CarbonCallback carbonCallback) throws Exception {
        this.outboundEndpoint.receive(carbonMessage, carbonCallback);
        return false;
    }


    /**
     * Call this method only when unHealthy LBEndpoint becomes Healthy.
     * <p>
     * NOTE: Have a lock on the LBOutboundEndpoint before calling this.
     */
    public void reset() {
        setHealthy(true);
        setHealthyRetriesCount(0);
        setUnHealthyRetriesCount(0);
        setHealthCheckedTime(0);

    }


}
