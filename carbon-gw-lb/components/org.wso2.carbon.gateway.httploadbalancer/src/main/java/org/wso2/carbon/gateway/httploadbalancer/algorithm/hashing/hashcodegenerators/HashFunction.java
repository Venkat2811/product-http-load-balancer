package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators;

/**
 * Interface for generating hashcode for IP hashing.
 * All methods of generating hashcode must implement this interface.
 */
public interface HashFunction {

    /**
     * @param value Any string for which hashcode is to be generated.
     * @return hashcode of the string.
     */
    String hash(String value);
}
