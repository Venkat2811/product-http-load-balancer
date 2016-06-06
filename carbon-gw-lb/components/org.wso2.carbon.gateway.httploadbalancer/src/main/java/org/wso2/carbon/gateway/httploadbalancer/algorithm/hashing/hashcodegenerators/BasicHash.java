package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators;

/**
 * Implementation of basic hashcode.
 * <p>
 * This is a very basic implementation and it is not recommended,
 * if you are expecting good hashcode for effective distribution of load.
 * <p>
 * However, if number of OutboundEndpoints are less you can use this.
 * But be sure to make all appropriate changes correctly.
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
