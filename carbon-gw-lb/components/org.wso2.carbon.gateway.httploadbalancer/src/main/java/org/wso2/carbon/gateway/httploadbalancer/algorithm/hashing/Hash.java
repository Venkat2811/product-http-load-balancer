package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing;


import java.util.List;

/**
 * Interface for IPHashing.
 * All types of hashing methods must implement this interface.
 */
public interface Hash {

    /**
     * @param endpoint add an endpoint of form <hostname:port>.
     */
    void addEndpoint(String endpoint);

    /**
     *
     * @param endpoints List of Endpoints of form <hostname:port> to be added.
     */
    void addEndpoints(List<String> endpoints);

    /**
     * @param endpoint remove an endpoint of form <hostname:port>.
     */
    void removeEndpoint(String endpoint);

    /**
     *
     * @param endpoints List of Endpoint of form <hostname:port> to be removed.
     */
    void removeAllEndpoints(List<String> endpoints);

    /**
     * @param ipAddress Client IP Address.
     * @return Chosen endpoint of form <hostname:port> based on hashing method.
     */
    String get(String ipAddress);
}
