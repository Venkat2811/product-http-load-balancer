package org.wso2.carbon.gateway.httploadbalancer.constants;

/**
 * Constants for Load Balancer.
 */
public class LoadBalancerConstants {

    /**
     * Config Keys.
     */
    public static final String ALGORITHM_NAME = "algorithmName";
    public static final String PERSISTENCE_TYPE = "persistenceType";

    // As of now, this timeout value will be used only with "LB_COOKIE" based persistence.
    public static final String PERSISTENCE_SESSION_TIME_OUT = "sessionTimeOut";

    public static final String SSL_TYPE = "sslType";

    // As of now, only passive is supported.
    public static final String HEALTH_CHECK_TYPE = "healthCheckType";

    // Amount of time for which LB has to wait for response from OutboundEndpoint.
    public static final String HEALTH_CHECK_REQUEST_TIMEOUT = "requestTimeout";

    // no of request failure or timeouts to mark an OutboundEndpoint as unhealthy.
    public static final String HEALTH_CHECK_UNHEALTHY_RETRIES = "unHealthyRetries";

    // no of successful responses to be received from an OutboundEndpoint to mark it as healthy again.
    public static final String HEALTH_CHECK_HEALTHY_RETRIES = "healthyRetries";

    // Scheduled time interval after which LB has to check if an Outboundendpoint is healthy again.
    public static final String HEALTH_CHECK_HEALTHY_CHECK_INTERVAL = "healthyCheckInterval";


    /**
     * LB Algorithm related Constants.
     */

    public static final String ROUND_ROBIN = "ROUND_ROBIN";
    public static final String LEAST_CONNECTIONS = "LEAST_CONNECTIONS";
    public static final String LEAST_RESPONSE_TIME = "LEAST_RESPONSE_TIME";


    /**
     * Session Persistence related Constants.
     */
    public static final String NO_PERSISTENCE = "NO_PERSISTENCE";
    public static final String APPLICATION_COOKIE = "APPLICATION_COOKIE";
    public static final String LB_COOKIE = "LB_COOKIE";
    public static final String CLIENT_IP_ADDRESS = "CLIENT_IP_ADDRESS";

    /**
     * SSL Support related Constants.
     */
    public static final String NO_SSL = "NO_SSL";
    public static final String SSL_OFFLOAD = "SSL_OFFLOAD";
    public static final String END_TO_END = "END_TO_END";

    /**
     * Health Checking related Constants.
     */
    public static final String PASSIVE_HEALTH_CHECK = "PASSIVE";
}
