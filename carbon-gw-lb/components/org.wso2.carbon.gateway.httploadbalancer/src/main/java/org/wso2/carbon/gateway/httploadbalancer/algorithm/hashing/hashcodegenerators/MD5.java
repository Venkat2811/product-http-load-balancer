package org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implementation of MD5.
 * <p>
 * HashCode generated using this provides good for effective load distribution.
 */
public class MD5 implements HashFunction {

    private static final Logger log = LoggerFactory.getLogger(MD5.class);

    MessageDigest md;

    /**
     * Default Constructor.
     */
    public MD5() {

        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.error(e.toString());
        }

    }

    /**
     * @param value Any string for which hashcode is to be generated.
     * @return MD5 hashcode for given string.
     */
    @Override
    public String hash(String value) {

        try {
            md.update(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.toString());
        }
        byte byteData[] = md.digest();

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }

        return result.toString();
    }
}
