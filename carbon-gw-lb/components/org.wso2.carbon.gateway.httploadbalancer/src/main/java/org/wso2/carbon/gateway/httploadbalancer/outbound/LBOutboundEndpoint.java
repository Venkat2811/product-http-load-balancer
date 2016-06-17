package org.wso2.carbon.gateway.httploadbalancer.outbound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
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

    /**
     * There attributes are for LeastResponseTime Algorithm.
     */
    private volatile int avgResponseTime = 0; // This stores running average.
    private volatile int percentage = 100;
    private volatile int maxRequestsPerWindow = 0;
    private int currentRequests = 0; // This stores current requests per window.

    /**
     * This ref to healthCheckCMsg will only be used for health Checking.
     * <p>
     * We will be trimming off all request related params from this carbonMessage.
     * So don't worry.
     */
    private CarbonMessage healthCheckCMsg;

    // HTTP or HTTPS Endpoint.
    private OutboundEndpoint outboundEndpoint;

    // Healthy or not.
    private boolean isHealthy = true;

    // No of retries to be done to see whether it is alive again.
    private int healthyRetriesCount = 0;

    // No of retries to be done to mark an endpoint as unHealthy.
    private int unHealthyRetriesCount = 0;

    // This will be used to hold System.nanoTime() value.
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

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public int getPercentage() {
        return this.percentage;
    }

    public int getCurrentRequests() {
        return currentRequests;
    }

    public void setCurrentRequests(int currentRequests) {
        this.currentRequests = currentRequests;
    }

    public int getMaxRequestsPerWindow() {
        return maxRequestsPerWindow;
    }

    public void setMaxRequestsPerWindow(int maxRequestsPerWindow) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
    }

    private void incrementCurrentRequests() {
        this.currentRequests++;
    }

    public Object getLock() {
        return this.lock;
    }

    /**
     *
     * @param newTime Most resent response time of the endpoint.
     * @return Running average of response time of that endpoint.
     *
     */
    public int computeAndSetAvgResponseTime(int newTime) {


        synchronized (this.lock) {

            if (this.avgResponseTime != 0) { //For first time we should not divide by 2.

                // log.info("ART : " + this.avgResponseTime + " LAT: " + newTime);
                if ((this.avgResponseTime + newTime) % 2 == 0) {
                    this.avgResponseTime = (this.avgResponseTime + newTime) / 2; // Dividing by 2.

                } else {
                    this.avgResponseTime = (((this.avgResponseTime + newTime) / 2) + 1);

                }

                //  log.info("ART : " + this.avgResponseTime);
            } else {

                this.avgResponseTime = newTime;
            }
        }
        return this.avgResponseTime;
    }

    public int getAvgResponseTime() {

        synchronized (lock) {
            return this.avgResponseTime;
        }
    }

    private void setHealthCheckCMsg(CarbonMessage healthCheckCMsg) {
        this.healthCheckCMsg = healthCheckCMsg;
    }

    public CarbonMessage getHealthCheckCMsg() {
        synchronized (lock) {
            return this.healthCheckCMsg;
        }
    }

    public OutboundEndpoint getOutboundEndpoint() {
        return outboundEndpoint;
    }

    public void setOutboundEndpoint(OutboundEndpoint outboundEndpoint) {
        this.outboundEndpoint = outboundEndpoint;
    }

    public boolean isHealthy() {
        synchronized (lock) {
            return isHealthy;
        }
    }

    public void setHealthy(boolean healthy) {

        isHealthy = healthy;

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

    public void setUnHealthyRetriesCount(int unHealthyRetriesCount) {
        this.unHealthyRetriesCount = unHealthyRetriesCount;

    }

    public long getHealthCheckedTime() {
        synchronized (lock) {
            return healthCheckedTime;
        }
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void setHealthCheckedTime(long healthCheckedTime) {

        this.healthCheckedTime = healthCheckedTime;
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

        this.outboundEndpoint.receive(carbonMessage, carbonCallback);

        synchronized (lock) {

            this.setHealthCheckCMsg(CommonUtil.getHealthCheckMessage(carbonMessage));

            if (context.getAlgorithm().equals(LoadBalancerConstants.LEAST_RESPONSE_TIME)) {

                this.incrementCurrentRequests();
            }


        }
        //No need to synchronize as we are operating on concurrent HashMap.
        context.addToCallBackPool(carbonCallback);

        return false;
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void incrementUnHealthyRetries() {


        this.unHealthyRetriesCount++;

        log.warn("Incremented UnHealthyRetries count for endPoint : " + this.getName());
    }

    public void incrementHealthyRetries() {

        synchronized (lock) {
            this.healthyRetriesCount++;
        }
        log.info("Incremented HealthyRetries count for endPoint : " + this.getName());
    }

    /**
     * NOTE: This method MUST be accessed after acquiring lock on lock Object.
     */
    public void markAsUnHealthy() {

        isHealthy = false;

        log.warn(this.getName() + " is unHealthy");
    }

    public void resetResponseTimeRelatedToDefault() {

        this.avgResponseTime = 0;
        this.percentage = 100;
        this.maxRequestsPerWindow = 0;
        this.currentRequests = 0;
    }

    /**
     * Call this method when unHealthy LBEndpoint becomes Healthy
     * and also when when we receive a successful response from backend
     * so that we can avoid any false alarms (i.e when unHealthy endpoint is
     * incremented due to some timeOut but the actual endpoint is actually healthy).
     * <p>
     */
    public void resetHealthPropertiesToDefault() {
        synchronized (lock) {
            this.isHealthy = true;
            this.unHealthyRetriesCount = 0;
            this.healthyRetriesCount = 0;
            this.healthCheckedTime = 0;

        }


    }


}
