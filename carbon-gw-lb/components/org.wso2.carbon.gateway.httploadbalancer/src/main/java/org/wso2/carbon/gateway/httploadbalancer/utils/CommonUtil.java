package org.wso2.carbon.gateway.httploadbalancer.utils;


import org.apache.commons.validator.routines.InetAddressValidator;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A util class for LB specific operations.
 */
public class CommonUtil {

    /**
     * @param map LBOutboundEndpoint map.
     * @return list of LBOutboundEndpoints.
     */
    public static List<LBOutboundEndpoint> getLBOutboundEndpointsList(Map<String, LBOutboundEndpoint> map) {

        return new ArrayList<>(map.values());
    }

    /**
     * @param lbOutboundEndpoints List of LBOutboundEndpoints map.
     * @return List of LBOutboundEndpoint names.
     */
    public static List<String> getLBOutboundEndpointNamesList(List<LBOutboundEndpoint> lbOutboundEndpoints) {

        ArrayList<String> names = new ArrayList<String>();

        for (LBOutboundEndpoint lbOutboundEndpoint : lbOutboundEndpoints) {
            names.add(lbOutboundEndpoint.getName());
        }
        return names;
    }

    /**
     * @param retries a string of form '<integer>times'.
     * @return <integer>.
     */
    public static int getRetriesCount(String retries) {
        int val;
        retries = retries.substring(0, retries.indexOf("t"));
        val = Integer.parseInt(retries);

        return val;
    }

    /**
     * @param time time in string '<integer>( h | m | s | ms )'.
     * @return <integer> in milli seconds.
     */
    public static int getTimeInMilliSeconds(String time) {
        int val = 0;

        if (time.contains("ms")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("m")));
            return val;

        } else if (time.contains("s")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("s")));
            return (int) TimeUnit.SECONDS.toMillis(val);

        } else if (time.contains("m")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("m")));
            return (int) TimeUnit.MINUTES.toMillis(val);

        } else if (time.contains("h")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("h")));
            return (int) TimeUnit.HOURS.toMillis(val);

        }

        return val;
    }

    /**
     * @param uri LBOutboundEndpoint's Uri.
     * @return <String> of form 'hostname:port'
     */
    public static String getHostAndPort(String uri) {

        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }
        String host = url.getHost();
        int port = (url.getPort() == -1) ? 80 : url.getPort();

        return host + ":" + String.valueOf(port);
    }

    public static String getUrlPath(String uri) {

        URL url = null;
        try {
            url = new URL(uri);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }

        return url.getPath();

    }

    public static String getCookieValue(CarbonMessage carbonMessage, LoadBalancerConfigContext context) {
        //Extracting host and port from response to identify appropriate cookie.

        return context.getCookieFromOutboundEP(
                carbonMessage.getProperty(Constants.HOST).toString() +
                        ":" + carbonMessage.getProperty(Constants.PORT).toString());
    }


    /**
     * @param cookieValue eg: EP1,EP2 etc.,
     * @return LB specific cookie.
     * <p>
     * NOTE: This will be used when there is no cookie from BE.
     * <p>
     * No scheduled is specified. So, there will be persistence until browser is closed.
     * So this is a Session Cookie.
     * <p>
     * //TODO: should we add path.?
     */
    public static String getSessionCookie(String cookieValue, boolean isSecure) {

        if (!isSecure) {
            return LoadBalancerConstants.LB_COOKIE_NAME + "=" + cookieValue +
                    "; " + LoadBalancerConstants.HTTP_ONLY;
        } else {
            return LoadBalancerConstants.LB_COOKIE_NAME + "=" + cookieValue +
                    "; " + LoadBalancerConstants.HTTP_ONLY + "; "
                    + LoadBalancerConstants.SECURE;
        }
    }

    /**
     * @param existingCookie existing cookie from BE.
     * @param lbCookieValue  eg: EP1,EP2 etc.,
     * @return BE cookie value appended with LB specific cookie.
     * <p>
     * The nature of this cookie (like HttpOnly, Age) is purely dependant on BE application server.
     * LB uses those properties on AS IS basis.
     */

    public static String addLBCookieToExistingCookie(String existingCookie, String lbCookieValue) {

        // Multiple fields of cookies are separated by ";"
        // So index of first ";" will give the index after 'value' attribute.
        // 'name' & 'value' attributes of cookie alone will be sent back by browser.
        // So, we have to append it in value field to maintain persistence.

        if (existingCookie.contains(";")) { //there are multiple fields in cookie.

            StringBuilder cookie = new StringBuilder();

            cookie.append(existingCookie.
                    substring(0, existingCookie.indexOf(";"))); // eg: JSESSIONID="sadfsad" is extracted.

            cookie.append(LoadBalancerConstants.LB_COOKIE_DELIMITER);
            cookie.append(LoadBalancerConstants.LB_COOKIE_NAME);
            cookie.append(LoadBalancerConstants.COOKIE_NAME_VALUE_SEPARATOR);
            cookie.append(lbCookieValue);
            cookie.append(LoadBalancerConstants.LB_COOKIE_DELIMITER);

            cookie.append(existingCookie.
                    substring(existingCookie.indexOf(";"), existingCookie.length())); // remaining cookie fields.

            return cookie.toString();

        } else { //there is no multiple fields in cookie.

            StringBuilder cookie = new StringBuilder();
            cookie.append(existingCookie);
            cookie.append(LoadBalancerConstants.LB_COOKIE_DELIMITER);
            cookie.append(LoadBalancerConstants.LB_COOKIE_NAME);
            cookie.append(LoadBalancerConstants.COOKIE_NAME_VALUE_SEPARATOR);
            cookie.append(lbCookieValue);
            cookie.append(LoadBalancerConstants.LB_COOKIE_DELIMITER);


            return cookie.toString();
        }

    }

    /**
     * @param cMsg Client's request.
     * @return Client's IPAddress.
     * <p>
     * It looks for the following HTTP request headers.
     * 1) X-Forwarded-For
     * 2) Client-IP
     * 3) Remote-Addr
     */
    public static String getClientIP(CarbonMessage cMsg) {

        //If client is behind proxy, this gives the best Client IP.
        if (cMsg.getHeader(LoadBalancerConstants.X_FORWARDED_FOR_HEADER) != null) {

            String ipList = cMsg.getHeader(LoadBalancerConstants.X_FORWARDED_FOR_HEADER);
            //The first IP in the list belongs to client.
            // eg: 192.168.72.3, 10.2.53.8, ..
            if (ipList.contains(",")) {

                return ipList.split(",", 2)[0].trim();
            } else {
                //There is only one IP
                return ipList;
            }

        } else if (cMsg.getHeader(LoadBalancerConstants.CLIENT_IP_HEADER) != null) {

            return cMsg.getHeader(LoadBalancerConstants.CLIENT_IP_HEADER);

        } else if (cMsg.getHeader(LoadBalancerConstants.REMOTE_ADDR_HEADER) != null) {

            return cMsg.getHeader(LoadBalancerConstants.REMOTE_ADDR_HEADER);
        }

        return null;
    }

    /**
     * @param ipAddress IPAddress retrieved from Client's request.
     * @return IPAddress is valid or not.
     * <p>
     * It checks for both IPv4 and IPv6 addresses.
     * <p>
     * This validation is not costly as doesn't makes any lookup or connection.
     * It is RegEx and String Validation. So don't worry.
     */
    public static boolean isValidIP(String ipAddress) {

        if (ipAddress != null) {
            return InetAddressValidator.getInstance().isValid(ipAddress);
        } else {
            return false;
        }

    }
}
