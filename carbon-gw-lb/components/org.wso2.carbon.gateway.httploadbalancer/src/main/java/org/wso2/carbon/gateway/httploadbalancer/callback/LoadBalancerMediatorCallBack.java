package org.wso2.carbon.gateway.httploadbalancer.callback;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.flow.Mediator;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Constants;

import java.util.concurrent.TimeUnit;


/**
 * Callback related to LoadBalancerMediator.
 * In case of cookie persistence, appropriate cookie will be appended with response here.
 */
public class LoadBalancerMediatorCallBack implements CarbonCallback {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerMediatorCallBack.class);

    //Incoming callback.
    private final CarbonCallback parentCallback;

    //LoadBalancer Mediator.
    private final Mediator mediator;

    //LoadBalancerConfigContext context.
    private final LoadBalancerConfigContext context;

    //LBOutboundEndpoint.
    //This will be used to locate specific lbOutboundEndpoint for healthChecking purposes.
    private final LBOutboundEndpoint lbOutboundEndpoint;

    //To store scheduled value for this callback.
    private final long timeOut;

    public long getTimeout() {

        return this.timeOut;
    }

    public LBOutboundEndpoint getLbOutboundEndpoint() {

        return this.lbOutboundEndpoint;
    }

    /**
     * Constructor.
     *
     * @param parentCallback CarbonCallback.
     * @param mediator       LoadBalancerMediator.
     */
    public LoadBalancerMediatorCallBack(CarbonCallback parentCallback, Mediator mediator,
                                        LoadBalancerConfigContext context, LBOutboundEndpoint lbOutboundEndpoint) {

        this.parentCallback = parentCallback;
        this.mediator = mediator;
        this.lbOutboundEndpoint = lbOutboundEndpoint;
        this.context = context;
        // Note that we are assigning scheduled value way ahead before invoking outboundEndpoint.
        // this will be atleast 2 to 5 milli second difference, which might cause removal of
        // object from pool before response arrives. So we are adding a grace period of 5 ms time to it.
        this.timeOut = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) -
                LoadBalancerConstants.DEFAULT_GRACE_PERIOD;

    }


    @Override
    public void done(CarbonMessage carbonMessage) {


        /**log.info("Inside LB mediator call back...");
         Map<String, String> transHeaders = carbonMessage.getHeaders();
         log.info("Transport Headers...");
         log.info(transHeaders.toString() + "\n\n");

         Map<String, Object> prop = carbonMessage.getProperties();
         log.info("Properties...");
         log.info(prop.toString());**/


        if (parentCallback instanceof LoadBalancerMediatorCallBack) {

            //Locking is not required as we are operating on ConcurrentHashMap.
            if (this.context.isInCallBackPool((CarbonCallback)
                    carbonMessage.getProperty(Constants.CALL_BACK))) {

                this.context.removeFromCallBackPool((CarbonCallback)
                        carbonMessage.getProperty(Constants.CALL_BACK));
                //From this point, this callback will not be available in pool.

                /**
                 * We are locking on this LBOutboundEndpoint object because,
                 * this might be used in LoadBalancerMediator and in TimeoutHandler.
                 *
                 * Since we are resetting the properties lock is must.
                 */
                synchronized (this.lbOutboundEndpoint) {
                    this.lbOutboundEndpoint.resetToDefault();
                }

            } else {
                log.error("Response received after removing callback from pool.." +
                        "This response will be discarded. " +
                        "You might have to adjust scheduled value to avoid this from happening.");
                return;
            }


            if (this.context.getPersistence().equals(LoadBalancerConstants.APPLICATION_COOKIE)) {

                /**
                 ///////////////////////////////////////////////////////////////////////////////////
                 //TODO: Just for testing. Remove this block later.
                 BasicClientCookie serverCookie = new BasicClientCookie("JSESSIONID", "ghsgsdgsg");

                 Date expiration = new Date(new Date().getTime() + 3600 * 1000);

                 serverCookie.setExpiryDate(expiration);

                 String finalCookie =
                 serverCookie.getName() + "=" + serverCookie.getValue()
                 +
                 "; expires =" + serverCookie.getExpiryDate() +
                 "; HTTPOnly";
                 carbonMessage.setHeader(LoadBalancerConstants.SET_COOKIE_HEADER, finalCookie);

                 ///////////////////////////////////////////////////////////////////////////////////////
                 **/


                /**Checking if there is any cookie already available in response from BE. **/

                if (carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE_HEADER) != null ||
                        carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE2_HEADER) != null) {
                    //Cookie exists.

                    //Appending LB_COOKIE along with existing cookie.
                    carbonMessage.setHeader(
                            (//Appending to appropriate header that is present.
                                    carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE_HEADER) != null ?
                                            LoadBalancerConstants.SET_COOKIE_HEADER :
                                            LoadBalancerConstants.SET_COOKIE2_HEADER
                            ),
                            CommonUtil.addLBCookieToExistingCookie(
                                    carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE_HEADER),
                                    CommonUtil.getCookieValue(carbonMessage, this.context)));

                } else { //There is no cookie in response from BE.

                    //Adding LB specific cookie.
                    log.error("BE endpoint doesn't has it's own cookie. LB will insert it's own cookie for " +
                            "the sake of maintaining persistence. ");

                    //Here we are not looking for Set-Cookie2 header coz, it is only for LB purpose.
                    //i.e., we are only inserting cookie. So using Set-Cookie itself.
                    carbonMessage.setHeader(LoadBalancerConstants.SET_COOKIE_HEADER,
                            CommonUtil.getSessionCookie(//here we are finding endpoint to insert appropriate cookie.
                                    CommonUtil.getCookieValue(carbonMessage, this.context), false));

                }


                parentCallback.done(carbonMessage);


            } else if (this.context.getPersistence().equals(LoadBalancerConstants.LB_COOKIE)) {

                /**
                 ///////////////////////////////////////////////////////////////////////////////////
                 //TODO: Just for testing. Remove this block later.
                 BasicClientCookie serverCookie = new BasicClientCookie("JSESSIONID", "ghsgsdgsg");

                 Date expiration = new Date(new Date().getTime() + 3600 * 1000);

                 serverCookie.setExpiryDate(expiration);

                 String finalCookie =
                 serverCookie.getName() + "=" + serverCookie.getValue();
                 // +
                 //   "; expires =" + serverCookie.getExpiryDate() +
                 //  "; HTTPOnly";
                 carbonMessage.setHeader(LoadBalancerConstants.SET_COOKIE_HEADER, finalCookie);

                 ///////////////////////////////////////////////////////////////////////////////////////
                 **/

                if (carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE_HEADER) != null ||
                        carbonMessage.getHeader(LoadBalancerConstants.SET_COOKIE2_HEADER) != null) {
                    //Cookie exists.

                    log.error("BE endpoint has it's own cookie, LB will DISCARD this. If" +
                            "you want your application cookie to be used, please choose " +
                            LoadBalancerConstants.APPLICATION_COOKIE +
                            " mode of persistence");
                }

                //Adding LB specific cookie.
                carbonMessage.setHeader(LoadBalancerConstants.SET_COOKIE_HEADER,
                        CommonUtil.getSessionCookie(//here we are finding endpoint to insert appropriate cookie.
                                CommonUtil.getCookieValue(carbonMessage, this.context), false));

                parentCallback.done(carbonMessage);

            } else { //for NO_PERSISTENCE and CLIENT_IP_HASHING type.

                parentCallback.done(carbonMessage);
            }

        } else if (mediator.hasNext()) { // If Mediator has a sibling after this

            try {
                mediator.next(carbonMessage, parentCallback);
            } catch (Exception e) {
                log.error("Error while mediating from Callback", e);
            }
        }

    }
}
