package org.wso2.carbon.gateway.httploadbalancer.utils;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * To get Objects (OutboundEndpoints) from HashMap in the form of List.
 */
public class MapToListConverter {

    public static List<OutboundEndpoint> getOutboundEndpointsList(Map<String, OutboundEndpoint> map) {

        return new ArrayList<>(map.values());
    }
}
