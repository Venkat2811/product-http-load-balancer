package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators;

/**
 * Implementation of basic hashcode.
 */
public class BasicHash implements HashFunction {


    /**
     * @param value Any string for which hashcode is to be generated.
     * @return Object.hashcode value for a given string.
     */
    @Override
    public String hash(String value) {

        return String.valueOf(value.hashCode());
    }
}
