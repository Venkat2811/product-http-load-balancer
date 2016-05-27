package org.wso2.carbon.gateway.httploadbalancer.utils;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A util class for LB.
 */
public class ConverterUtil {

    /**
     * @param map OutboundEndpoint map.
     * @return list of Outboundundpoints.
     */
    public static List<OutboundEndpoint> getOutboundEndpointsList(Map<String, OutboundEndpoint> map) {

        return new ArrayList<>(map.values());
    }

    /**
     *
     * @param retries a string of form '<integer>times'
     * @return <integer>
     */
    public static int getRetriesCount(String retries) {
        int val;
        retries = retries.substring(0, retries.indexOf("t"));
        val = Integer.parseInt(retries);

        return val;
    }

}
