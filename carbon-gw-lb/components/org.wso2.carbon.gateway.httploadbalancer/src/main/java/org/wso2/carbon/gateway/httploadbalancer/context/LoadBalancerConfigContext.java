package org.wso2.carbon.gateway.httploadbalancer.context;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.simple.StrictClientIPHashing;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonCallback;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Holds LB Configuration context.
 * <p>
 * Context object will be passed to mediators so that they can use config whenever necessary.
 */
public class LoadBalancerConfigContext {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfigContext.class);

    private String algorithm;

    private String persistence;
    // private int sessionPersistenceTimeout; //TODO: Discuss.

    private String sslType;

    //There are the values as specified in config.
    private String healthCheck;
    private int reqTimeout;
    private int unHealthyRetries;
    private int healthyRetries;
    private int healthycheckInterval;
    private String testRequest;


    private Map<String, LBOutboundEndpoint> lbOutboundEndpoints;

    //TODO: Is this HashMap idea okay.?
    /**
     * Used to identify corresponding BackEnd Endpoint Key for a given cookie.
     * <p>
     * This map will be used when request comes from Client -> LB
     * and based on cookie, BE endpoint will be chosen.
     * Each cookie value will point to a BE endpoint. Eg: EP1,EP2 etc.,
     * <p>
     * TODO:If there is any security concern, any other meaningless string
     * TODO:can be stored instead of EP1,EP2 etc.
     */
    private Map<String, String> cookieToEPKeyMap;

    /**
     * Used to identify corresponding Cookie for a given BackEnd Endpoint.
     * <p>
     * This map will be used once response arrives from BE and
     * to use appropriate cookie for the endpoint.
     * <p>
     * NOTE: EndpointName will be of the form <hostName:port>
     */
    private Map<String, String> endpointToCookieMap;

    /**
     * A list that holds all unHealthyLBOutboundEndpoints.
     */
    private final Queue<LBOutboundEndpoint> unHealthyLBEPQueue = new ConcurrentLinkedQueue<>();

    /**
     * A LoadBalancerMediatorCallBack Pool for storing active callbacks.
     * <p>
     * NOTE: This pool stores only LoadBalancerMediatorCallBack objects.
     * <p>
     * TimeoutHandler will use this pool to check for Timeout of requests.
     * <p>
     * Accessing Pool objects MUST be synchronized.
     */
    private final Map<String, CarbonCallback> callBackPool = new ConcurrentHashMap<>();

    /**
     * This will be used if persistence is chosen as CLIENT_IP_HASHING.
     */
    private StrictClientIPHashing strictClientIPHashing;


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

    public String getTestRequest() {
        return testRequest;
    }

    public void setTestRequest(String testRequest) {
        this.testRequest = testRequest;
    }

    /**
     * This method MUST be called before accessing cookie related maps.
     */
    public void initCookieMaps() {

        cookieToEPKeyMap = new ConcurrentHashMap<>();
        endpointToCookieMap = new ConcurrentHashMap<>();
    }

    public void initStrictClientIPHashing(List<LBOutboundEndpoint> lbOutboundEndpoints) {
        strictClientIPHashing = new StrictClientIPHashing(lbOutboundEndpoints);
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

    public Map<String, LBOutboundEndpoint> getLbOutboundEndpoints() {
        return lbOutboundEndpoints;
    }

    public void setLbOutboundEndpoints(Map<String, LBOutboundEndpoint> lbOutboundEndpoints) {
        this.lbOutboundEndpoints = lbOutboundEndpoints;
    }

    /**
     * @param name LBOutboundEndpoint's name.
     * @return Corresponding LBOutboundEndpoint object.
     */
    public LBOutboundEndpoint getLBOutboundEndpoint(String name) {

        return this.lbOutboundEndpoints.get(name);
    }

    public int getUnHealthyEPQueueSize() {

        return this.unHealthyLBEPQueue.size();
    }

    public Queue<LBOutboundEndpoint> getUnHealthyLBEPQueue() {
        return unHealthyLBEPQueue;
    }

    /**
     * @param lbOutboundEndpoint UnHealthyLBOutboundEndpoint to be added to the list.
     *                           <p>
     *                           NOTE: always access this method with having lock on
     *                           unHealthyLBEPQueue object.
     *                           Add to the list only after checking using
     *                           isAlreadyInUnHealthyList() method
     */
    public void addToUnHealthyList(LBOutboundEndpoint lbOutboundEndpoint) {

        this.unHealthyLBEPQueue.add(lbOutboundEndpoint);
    }

    /**
     * @param lbOutboundEndpoint To check whether it is already in list or not.
     *                           <p>
     *                           NOTE: always access this method with having lock on
     *                           unHealthyLBEPQueue  object.
     * @return existing or not.
     */
    public boolean isAlreadyInUnHealthyList(LBOutboundEndpoint lbOutboundEndpoint) {

        if (this.unHealthyLBEPQueue.contains(lbOutboundEndpoint)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param lbOutboundEndpoint UnHealthyLBOutboundEndpoint to be removed from list.
     *                           <p>
     *                           NOTE: always access this method with having lock on
     *                           unHealthyLBEPQueue object.
     *                           Remove from list only after checking using
     *                           isAlreadyInUnHealthyList() method
     */
    public void removeFromUnhealthyList(LBOutboundEndpoint lbOutboundEndpoint) {

        this.unHealthyLBEPQueue.remove(lbOutboundEndpoint);
    }


    public Map<String, CarbonCallback> getCallBackPool() {

        return callBackPool;
    }


    /**
     * @param callback LoadBalancerMediatorCallBack.
     *                 <p>
     *                 NOTE: This operation is always thread safe.
     */
    public void addToCallBackPool(CarbonCallback callback) {

        String name = callback.toString().substring(callback.toString().lastIndexOf(".") + 1,
                callback.toString().length());

        this.callBackPool.putIfAbsent(name, callback);

      //  log.info("Added to pool Key : " + name);

    }


    /**
     * @param callback LoadBalancerMediatorCallBack.
     * @return present or not.
     * <p>
     * NOTE: This operation is always thread safe.
     */
    public boolean isInCallBackPool(CarbonCallback callback) {

        String name = callback.toString().substring(callback.toString().lastIndexOf(".") + 1,
                callback.toString().length());

        if (this.callBackPool.containsKey(name)) {
          //  log.info("Is in Pool : " + name);
            return true;
        } else {
          //  log.info("Is not in Pool : " + name);
            return false;
        }
    }


    /**
     * @param callback LoadBalancerMediatorCallBack.
     *                 <p>
     *                 NOTE: This operation is always thread safe.
     */
    public void removeFromCallBackPool(CarbonCallback callback) {

        String name = callback.toString().substring(callback.toString().lastIndexOf(".") + 1,
                callback.toString().length());

       // log.info("Removing from Pool  Key : " + name);

        this.callBackPool.remove(name);


    }

    public StrictClientIPHashing getStrictClientIPHashing() {
        return strictClientIPHashing;
    }
}
