package org.wso2.carbon.gateway.httploadbalancer.algorithm;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds LB Configuration context.
 */
public class LoadBalancerConfigContext {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfigContext.class);

    private String algorithm;

    private String persistence;
    // private int sessionPersistenceTimeout; //TODO: Discuss.

    private String sslType;

    private String healthCheck;
    private int reqTimeout;
    private int unHealthyRetries;
    private int healthyRetries;
    private int healthycheckInterval;

    private Map<String, OutboundEndpoint> outboundEndpoints;

    //TODO: Is this HashMap idea okay.?
    /**
     * Used to identify corresponding BackEnd Endpoint Key for a given cookie.
     * This map will be used when request comes from Client -> LB
     * and based on cookie, BE endpoint will be chosen.
     * Each cookie value will point to BE. Eg: EP1,EP2 etc.,
     * TODO:If there is any security concern, ObjectID or any other meaningless string
     * TODO:can be stored instead of EP1,EP2 etc.
     */
    private Map<String, String> cookieToEPKeyMap;

    /**
     * Used to identify corresponding Cookie for a given BackEnd Endpoint.
     * This map will be used once response arrives from BE and
     * to use appropriate cookie for the endpoint.
     * NOTE: EndpointName will be of the form <hostName:port>
     */
    private Map<String, String> endpointToCookieMap;


    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPersistence() {
        return persistence;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    /**
     * TODO:Discuss.
     * public int getSessionPersistenceTimeout() {
     * return sessionPersistenceTimeout;
     * }
     * <p>
     * public void setSessionPersistenceTimeout(int sessionPersistenceTimeout) {
     * this.sessionPersistenceTimeout = sessionPersistenceTimeout;
     * }
     **/

    public String getSslType() {
        return sslType;
    }

    public void setSslType(String sslType) {
        this.sslType = sslType;
    }

    public String getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(String healthCheck) {
        this.healthCheck = healthCheck;
    }

    public int getReqTimeout() {
        return reqTimeout;
    }

    public void setReqTimeout(int reqTimeout) {
        this.reqTimeout = reqTimeout;
    }

    public int getUnHealthyRetries() {
        return unHealthyRetries;
    }

    public void setUnHealthyRetries(int unHealthyRetries) {
        this.unHealthyRetries = unHealthyRetries;
    }

    public int getHealthyRetries() {
        return healthyRetries;
    }

    public void setHealthyRetries(int healthyRetries) {
        this.healthyRetries = healthyRetries;
    }

    public int getHealthycheckInterval() {
        return healthycheckInterval;
    }

    public void setHealthycheckInterval(int healthycheckInterval) {
        this.healthycheckInterval = healthycheckInterval;
    }

    public void initCookieMaps() {

        cookieToEPKeyMap = new ConcurrentHashMap<>();
        endpointToCookieMap = new ConcurrentHashMap<>();
    }

    /**
     * @param cookieName
     * @param outboundEPKey Maps cookie to an outbound EP.
     */
    public void addToCookieToOutboundEPKeyMap(String cookieName, String outboundEPKey) {

        cookieToEPKeyMap.put(cookieName, outboundEPKey);

    }

    /**
     * @param cookieName
     * @return OutboundEndpointKey.
     * Returns an OutboundEndpointKey for a given cookieName.
     */
    public String getOutboundEPKeyFromCookie(String cookieName) {

        return cookieToEPKeyMap.get(cookieName);
    }


    /**
     * @param endpoint
     * @param cookieName Maps OutboundEP to a cookieName.
     */
    public void addToOutboundEPTOCookieMap(String endpoint, String cookieName) {

        endpointToCookieMap.put(endpoint, cookieName);

    }

    /**
     * @param endpoint
     * @return CookieName.
     * Returns a cookie for a given OutboundEP.
     */
    public String getCookieFromOutboundEP(String endpoint) {

        return endpointToCookieMap.get(endpoint);
    }

    public Map<String, OutboundEndpoint> getOutboundEndpoints() {
        return outboundEndpoints;
    }

    public void setOutboundEndpoints(Map<String, OutboundEndpoint> outboundEndpoints) {
        this.outboundEndpoints = outboundEndpoints;
    }

    public OutboundEndpoint getOutboundEndpoint(String name) {
        return this.outboundEndpoints.get(name);
    }
}
