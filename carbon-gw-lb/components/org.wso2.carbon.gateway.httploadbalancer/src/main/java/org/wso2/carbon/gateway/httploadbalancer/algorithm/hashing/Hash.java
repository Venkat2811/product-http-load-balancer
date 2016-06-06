package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing;


import java.util.List;

/**
 * Interface for IPHashing.
 * All types of hashing methods must implement this interface.
 */
public interface Hash {

    /**
     * @param endpoint add an endpoint.
     */
    void addEndpoint(String endpoint);

    /**
     *
     * @param endpoints List of Endpoint names to be added.
     */
    void addEndpoints(List<String> endpoints);

    /**
     * @param endpoint remove an endpoint.
     */
    void removeEndpoint(String endpoint);

    /**
     *
     * @param endpoints List of Endpoint names to be removed.
     */
    void removeAllEndpoints(List<String> endpoints);

    /**
     * @param ipAddress Client IP Address.
     * @return Chosen endpoint based on hashing method.
     */
    String get(String ipAddress);
}
