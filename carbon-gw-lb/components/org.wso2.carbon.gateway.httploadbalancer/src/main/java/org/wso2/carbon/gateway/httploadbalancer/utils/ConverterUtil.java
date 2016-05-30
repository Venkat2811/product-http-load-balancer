package org.wso2.carbon.gateway.httploadbalancer.utils;

import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;

import java.net.MalformedURLException;
import java.net.URL;
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
            val *= 1000;
            return val;

        } else if (time.contains("m")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("m")));
            val *= 60 * 1000;
            return val;

        } else if (time.contains("h")) {

            val = Integer.parseInt(time.substring(0, time.indexOf("h")));
            val *= 60 * 60 * 1000;

        }

        return val;
    }

    /**
     * @param uri OutboundEndpoint's Uri.
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
}
