package org.wso2.carbon.gateway.httploadbalancer.algorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.ConsistentHash;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.Hash;
import org.wso2.carbon.gateway.httploadbalancer.algorithm.hashing.hashcodegenerators.MD5;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
import org.wso2.carbon.messaging.CarbonMessage;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of Client IP based Hashing.
 * <p>
 * All Endpoints are assumed to have equal weights.
 * <p>
 * This algorithm identifies Client's IP address from HTTP header and applies a hashing method.
 * So as long as the client's IP is same, same backend endpoint will be chosen.
 * <p>
 * This method is not quite effective because, clients behind a proxy (organization etc.,)
 * will have same IP. Since all those requests will be directed to the same endpoint, there
 * won't be effective load distribution.
 * <p>
 * To know more about hashing, kindly look at the comments in respective classes.
 * <p>
 * This algorithm by-itself maintains persistence. So, while choosing this algorithm,
 * persistence should be specified as NO_PERSISTENCE.
 */
public class ClientIPHashing implements LoadBalancingAlgorithm {

    private static final Logger log = LoggerFactory.getLogger(ClientIPHashing.class);
    private final Object lock = new Object();

    private List<OutboundEndpoint> outboundEndpoints;
    private Hash hash;

    /**
     * Default Constructor.
     */
    public ClientIPHashing() {

    }

    /**
     * Constructor.
     *
     * @param outboundEndpoints List of OutboundEndpoints.
     */
    public ClientIPHashing(List<OutboundEndpoint> outboundEndpoints) {

        synchronized (lock) {

            this.outboundEndpoints = outboundEndpoints;
            /**
             * Two points are to be noted here.
             *
             * 1) You can also implement your own hashing mechanism. Eg: ModuloHash.
             *
             * 2) ConsistentHash needs a HashFunction.  We are using MD5 here. Another example is BasicHash.
             *    You can also implement your own HashFunction.
             */
            this.hash = new ConsistentHash(new MD5(),
                    CommonUtil.getOutboundEndpointNamesList(this.outboundEndpoints));
        }
    }

    /**
     * @return Algorithm name.
     */
    @Override
    public String getName() {

        return LoadBalancerConstants.IP_HASHING;
    }

    /**
     * @param outboundEPs list of all Outbound Endpoints to be load balanced.
     */
    @Override
    public void setOutboundEndpoints(List<OutboundEndpoint> outboundEPs) {

        synchronized (lock) {
            this.outboundEndpoints = outboundEPs;
            /**
             * Two points are to be noted here.
             *
             * 1) You can also implement your own hashing mechanism. Eg: ModuloHash.
             *
             * 2) ConsistentHash needs a HashFunction.  We are using MD5 here. Another example is BasicHash.
             *    You can also implement your own HashFunction.
             */
            this.hash = new ConsistentHash(new MD5(),
                    CommonUtil.getOutboundEndpointNamesList(this.outboundEndpoints));
        }
    }

    /**
     * @param outboundEndpoint outboundEndpoint to be added to the existing list.
     */
    @Override
    public void addOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        synchronized (lock) {
            outboundEndpoints.add(outboundEndpoint);
            hash.addEndpoint(outboundEndpoint.getName());
        }

    }

    /**
     * @param outboundEndpoint outboundEndpoint to be removed from existing list.
     */
    @Override
    public void removeOutboundEndpoint(OutboundEndpoint outboundEndpoint) {

        synchronized (lock) {
            outboundEndpoints.remove(outboundEndpoint);
            hash.removeEndpoint(outboundEndpoint.getName());
        }

    }

    /**
     * @param cMsg    Carbon Message has all headers required to make decision.
     * @param context LoadBalancerConfigContext.
     * @return OutboundEndpoint Object.
     */
    @Override
    public OutboundEndpoint getNextOutboundEndpoint(CarbonMessage cMsg, LoadBalancerConfigContext context) {

        OutboundEndpoint endPoint = null;

        synchronized (lock) {
            if (outboundEndpoints != null && outboundEndpoints.size() > 0) {

                String ipAddress = CommonUtil.getClientIP(cMsg);
                log.info("IP address retrieved is : " + ipAddress);
                if (CommonUtil.isValidIP(ipAddress)) {

                    endPoint = context.getOutboundEndpoint(hash.get(ipAddress));

                } else {

                    log.error("The IP Address retrieved is : " + ipAddress +
                            " which is invalid according to our validation.");
                    //TODO: throw appropriate exceptions also.

                }

            } else {

                log.error("No outbound end point is available..");
                //TODO: throw appropriate exceptions also.

            }
        }


        return endPoint;
    }

    /**
     * Hash Circle will be re-constructed in case of ConsistentHash.
     */
    @Override
    public void reset() {

        synchronized (lock) {
            hash.removeAllEndpoints(CommonUtil.getOutboundEndpointNamesList(outboundEndpoints));
            hash.addEndpoints(CommonUtil.getOutboundEndpointNamesList(outboundEndpoints));

        }
    }
}
