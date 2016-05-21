package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;

/**
 * Implementation of Round Robin Algorithm.
 * All Endpoints are assumed to have equal weights.
 * TODO: Check whether this is thread safe.  Think about groups also.
 */
public class RoundRobin implements LoadBalanceAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(RoundRobin.class);

    private int index = 0;
    private int endPointsCount = 0;

    private List<OutboundEndpoint> outboundEndpoints;


    public RoundRobin() {

    }

    public RoundRobin(List<OutboundEndpoint> outboundEndpoints) {
        this.outboundEndpoints = outboundEndpoints;
        endPointsCount = outboundEndpoints.size();
    }


    @Override
    public String getName() {

        return LoadBalancerConstants.ROUND_ROBIN;
    }

    @Override
    public void setOutboundEndpoints(List<OutboundEndpoint> outboundEndpoints) {

        this.outboundEndpoints = outboundEndpoints;
        endPointsCount = outboundEndpoints.size();

    }

    /**
     * @param cMsg Carbon Message has all headers required to make decision.
     * @return chosen Outboundendpoint
     * <p>
     * TODO: Support persistence also. Before choosing an endpoint check whether it is healthy or not.
     */

    @Override
    public OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg) {

        OutboundEndpoint endPoint = null;

        if (outboundEndpoints != null && outboundEndpoints.size() > 0) {

            endPoint = outboundEndpoints.get(index);
            index++;
            index %= endPointsCount;

        } else {

            log.error("No outbound end point is available..");
            //TODO: throw appropriate exceptions also.

        }


        return endPoint;
    }

    @Override
    public void reset() {

        index = 0;
    }
}
