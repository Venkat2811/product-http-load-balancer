package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;
import org.wso2.carbon.messaging.CarbonCallback;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Constants;
import org.wso2.carbon.messaging.DefaultCarbonMessage;
import org.wso2.carbon.messaging.FaultHandler;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Error Handler For LoadBalancer.
 */

public class LBErrorHandler implements FaultHandler {

    private static final Logger log = LoggerFactory.getLogger(LBErrorHandler.class);


    /**
     * @param errorCode      HTTP Error Code.
     * @param throwable      Throwable instance of error.
     * @param response       Must be DefaultCarbonMessage.
     * @param carbonCallback CallBackObject for sending response.
     *                       <p>
     *                       NOTE: As of now, we will send empty DefaultCarbonMessage.
     *                       It is for future use. Eg: From user request, we can findOut
     *                       Content-Type. So error response can also be sent of that
     *                       Content-Type and Encoding.
     */
    @Override
    public void handleFault(String errorCode, Throwable throwable, CarbonMessage response,
                            CarbonCallback carbonCallback) {


        String payload = throwable.getMessage();

        ((DefaultCarbonMessage) response).setStringMessageBody(payload);

        byte[] errorMessageBytes = payload.getBytes(Charset.defaultCharset());

        Map<String, String> transportHeaders = new HashMap<>();
        transportHeaders.put(org.wso2.carbon.transport.http.netty.common.Constants.HTTP_CONNECTION,
                org.wso2.carbon.transport.http.netty.common.Constants.KEEP_ALIVE);
        transportHeaders.put(org.wso2.carbon.transport.http.netty.common.Constants.HTTP_CONTENT_ENCODING,
                org.wso2.carbon.transport.http.netty.common.Constants.GZIP);
        transportHeaders.put(org.wso2.carbon.transport.http.netty.common.Constants.HTTP_CONTENT_TYPE,
                org.wso2.carbon.transport.http.netty.common.Constants.TEXT_PLAIN);
        transportHeaders.put(org.wso2.carbon.transport.http.netty.common.Constants.HTTP_CONTENT_LENGTH,
                (String.valueOf(errorMessageBytes.length)));
        transportHeaders.put(Constants.ERROR_MESSAGE, payload);
        transportHeaders.put(Constants.ERROR_DETAIL, payload);
        transportHeaders.put(Constants.ERROR_CODE, errorCode);
        transportHeaders.put(org.wso2.carbon.transport.http.netty.common.Constants.HTTP_STATUS_CODE, errorCode);

        response.setHeaders(transportHeaders);
        response = CommonUtil.appendLBIP(response, false);


        if (carbonCallback instanceof LoadBalancerMediatorCallBack) {

            ((LoadBalancerMediatorCallBack) carbonCallback).getParentCallback().done(response);

        } else {

            carbonCallback.done(response);
        }

    }


}
