package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;

/**
 * Implementation of Simple Round Robin Algorithm.
 * All Endpoints are assumed to have equal weights.
 * TODO: To be implemented.
 */
public class RoundRobin implements LoadBalanceAlgorithm {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setOutboundEndpoints(List<OutboundEndpoint> outboundEPs) {

    }

    @Override
    public OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg) {
        return null;
    }

    @Override
    public void reset() {

    }
}
