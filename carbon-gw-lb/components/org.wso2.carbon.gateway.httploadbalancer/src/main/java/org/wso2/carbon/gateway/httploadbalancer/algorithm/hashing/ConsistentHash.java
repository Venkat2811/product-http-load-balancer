package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing;

import org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators.HashFunction;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators.MD5;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Implementation of Consistent Hash for ClientIPHashing based Load balancing.
 * TODO: explanation and reference urls.
 */
public class ConsistentHash implements Hash {

    private final HashFunction hashFunction;
    private final SortedMap<String, String> circle = new TreeMap<>();


    /**
     * @param endpoints List of OutboundEndpoints.
     *                  <p>
     *                  MD5 hash function will be used.
     *                  <p>
     *                  If you want different hash function implementation use the
     *                  other constructor.
     */
    public ConsistentHash(List<String> endpoints) {

        this.hashFunction = new MD5();
        endpoints.forEach(this::addEndpoint);
    }

    /**
     * @param hashFunction Any custom implementation of hashFunction.
     *                     <p>
     *                     You can also implement your own by implementing HashFunction interface.
     * @param endpoints    List of OutboundEndpoints.
     */
    public ConsistentHash(HashFunction hashFunction, List<String> endpoints) {

        this.hashFunction = hashFunction;
        endpoints.forEach(this::addEndpoint);
    }


    /**
     * @param endpoint add an endpoint.
     */
    @Override
    public void addEndpoint(String endpoint) {

        if (LoadBalancerConstants.NUM_OF_REPLICAS == 1) {

            circle.put(hashFunction.hash(endpoint), endpoint);

        } else {

            for (int i = 0; i < LoadBalancerConstants.NUM_OF_REPLICAS; i++) {
                circle.put(hashFunction.hash(endpoint + i), endpoint);
            }
        }

    }

    /**
     * @param endpoints List of Endpoint names to be added.
     */
    @Override
    public void addEndpoints(List<String> endpoints) {
        endpoints.forEach(this::addEndpoint);
    }


    /**
     * @param endpoint remove an endpoint.
     */
    @Override
    public void removeEndpoint(String endpoint) {

        if (LoadBalancerConstants.NUM_OF_REPLICAS == 1) {

            circle.remove(hashFunction.hash(endpoint));

        } else {

            for (int i = 0; i < LoadBalancerConstants.NUM_OF_REPLICAS; i++) {
                circle.remove(hashFunction.hash(endpoint + i));
            }
        }

    }

    /**
     *
     * @param endpoints List of Endpoint names to be removed.
     */
    @Override
    public void removeAllEndpoints(List<String> endpoints) {
        endpoints.forEach(this::removeEndpoint);
    }


    /**
     * @param ipAddress Client IP Address.
     * @return Chosen Endpoint based on HashFunction implementation.
     */
    @Override
    public String get(String ipAddress) {
        if (circle.isEmpty()) {
            return null;
        }

        String hash = hashFunction.hash(ipAddress);

        if (!circle.containsKey(hash)) {

            SortedMap<String, String> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }

        return circle.get(hash);
    }
}
