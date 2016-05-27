package org.wso2.carbon.gateway.httploadbalancer.algorithm;

/**
 * Holds LB Configuration context.
 */
public class LoadBalancerConfigContext {

    private String algorithm;

    private String persistence;
    private int sessionPersistenceTimeout; // TODO: decide int or long

    private String sslType;

    private String healthCheck;
    private int reqTimeout; // TODO: decide int or long
    private int unHealthyRetries;
    private int healthyRetries;
    private int healthycheckInterval; // TODO: decide int or long


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

    public int getSessionPersistenceTimeout() {
        return sessionPersistenceTimeout;
    }

    public void setSessionPersistenceTimeout(int sessionPersistenceTimeout) {
        this.sessionPersistenceTimeout = sessionPersistenceTimeout;
    }

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
}
