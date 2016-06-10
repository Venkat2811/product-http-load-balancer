package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.httploadbalancer.callback.LoadBalancerMediatorCallBack;
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


    @Override
    public void handleFault(String errorCode, Throwable throwable, CarbonMessage carbonMessage,
                            CarbonCallback carbonCallback) {

        DefaultCarbonMessage response = new DefaultCarbonMessage();
        String payload = throwable.getMessage();

        response.setStringMessageBody(payload);
        byte[] errorMessageBytes = payload.getBytes(Charset.defaultCharset());

        Map<String, String> transportHeaders = new HashMap<>();
        transportHeaders.put(Constants.HTTP_CONNECTION, Constants.KEEP_ALIVE);
        transportHeaders.put(Constants.HTTP_CONTENT_ENCODING, Constants.GZIP);
        transportHeaders.put(Constants.HTTP_CONTENT_TYPE, Constants.TEXT_PLAIN);
        transportHeaders.put(Constants.HTTP_CONTENT_LENGTH,
                (String.valueOf(errorMessageBytes.length)));
        transportHeaders.put(Constants.HTTP_STATUS_CODE, errorCode);

        response.setHeaders(transportHeaders);


        if (carbonCallback instanceof LoadBalancerMediatorCallBack) {

            ((LoadBalancerMediatorCallBack) carbonCallback).getParentCallback().done(response);

        } else {

            carbonCallback.done(response);
        }

    }


}
