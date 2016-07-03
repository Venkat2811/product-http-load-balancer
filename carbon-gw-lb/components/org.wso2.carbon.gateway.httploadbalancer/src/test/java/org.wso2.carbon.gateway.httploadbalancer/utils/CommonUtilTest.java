package org.wso2.carbon.gateway.httploadbalancer.utils;

import org.junit.Assert;
import org.junit.Test;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.DefaultCarbonMessage;


/**
 * Unit Test for CommonUtil Class.
 */
public class CommonUtilTest {

    @Test
    public void testGetTimeInMilliSeconds() {

        Assert.assertEquals(1, CommonUtil.getTimeInMilliSeconds("1ms"));
        Assert.assertEquals(9000, CommonUtil.getTimeInMilliSeconds("9s"));
        Assert.assertEquals(300000, CommonUtil.getTimeInMilliSeconds("5m"));
        Assert.assertEquals(3600000, CommonUtil.getTimeInMilliSeconds("1h"));
    }

    @Test
    public void testGetHostAndPort() {

        Assert.assertEquals("localhost:8080", CommonUtil.getHostAndPort("http://localhost:8080/stockquote/all"));
        Assert.assertEquals(null, CommonUtil.getHostAndPort("http:/www.google.com"));
        Assert.assertEquals("www.google.com:80", CommonUtil.getHostAndPort("http://www.google.com"));
        //This case will cause java.net.URISyntaxException
        Assert.assertEquals(null, CommonUtil.getHostAndPort("http://localhost:8080/stockquote\\all"));
        Assert.assertEquals(null, CommonUtil.getHostAndPort("htp:://localhost:8080/stockquote/all"));
    }

    @Test
    public void testGetHost() {

        Assert.assertEquals("localhost", CommonUtil.getHost("http://localhost:8080/stockquote/all"));
        Assert.assertEquals(null, CommonUtil.getHost("http:/www.google.com"));
        Assert.assertEquals("www.google.com", CommonUtil.getHost("http://www.google.com"));
        //This case will cause java.net.URISyntaxException
        Assert.assertEquals(null, CommonUtil.getHost("http://localhost:8080/stockquote\\all"));
        Assert.assertEquals(null, CommonUtil.getHost("htp:://localhost:8080/stockquote/all"));

    }

    @Test
    public void testGetPort() {

        Assert.assertEquals(8080, CommonUtil.getPort("http://localhost:8080/stockquote/all"));
        Assert.assertEquals(-1, CommonUtil.getPort("http:/www.google.com"));
        Assert.assertEquals(-1, CommonUtil.getPort("http://www.google.com"));
        //This case will cause java.net.URISyntaxException
        Assert.assertEquals(-1, CommonUtil.getPort("http://localhost:8080/stockquote\\all"));
        Assert.assertEquals(-1, CommonUtil.getPort("htp:://localhost:8080/stockquote/all"));

    }

    @Test
    public void testGetClientIP() {

        CarbonMessage carbonMessage = new DefaultCarbonMessage();

        Assert.assertEquals(null, CommonUtil.getClientIP(carbonMessage));

        carbonMessage.setHeader("Client-IP", "192.165.87.23");
        Assert.assertEquals("192.165.87.23", CommonUtil.getClientIP(carbonMessage));

        //We are removing Client-IP header, so value of Remote-Addr should be returned.
        carbonMessage.removeHeader("Client-IP");
        carbonMessage.setHeader("Remote-Addr", "192.165.87.23");
        Assert.assertEquals("192.165.87.23", CommonUtil.getClientIP(carbonMessage));

        //If Client-IP and Remote-Addr are present, value of Client-IP should be returned.
        carbonMessage.setHeader("Client-IP", "10.11.12.13");
        Assert.assertEquals("10.11.12.13", CommonUtil.getClientIP(carbonMessage));

        //If X-Forwarded-For, Client-IP and Remote-Addr are present value of X-Forwarded-For should be returned.
        carbonMessage.setHeader("X-Forwarded-For", "19.165.87.23");
        Assert.assertEquals("19.165.87.23", CommonUtil.getClientIP(carbonMessage));

        //First IP in X-Forwarded-For has to be returned.
        carbonMessage.setHeader("X-Forwarded-For", "90.89.88.87,19.165.87.23");
        Assert.assertEquals("90.89.88.87", CommonUtil.getClientIP(carbonMessage));

    }

    @Test
    public void testIsValidIP() {

        Assert.assertEquals(false, CommonUtil.isValidIP(null));

        //IPv4
        Assert.assertEquals(false, CommonUtil.isValidIP("192.132.43"));
        Assert.assertEquals(true, CommonUtil.isValidIP("1.1.1.1"));
        Assert.assertEquals(false, CommonUtil.isValidIP("1.1.1.1."));
        Assert.assertEquals(false, CommonUtil.isValidIP("257.23.1.193"));
        Assert.assertEquals(false, CommonUtil.isValidIP(" 255.23.1.193")); //Space is not allowed.

        //IPv6
        Assert.assertEquals(true, CommonUtil.isValidIP("2001:0db8:0a0b:12f0:0000:0000:0000:0001"));
        Assert.assertEquals(true, CommonUtil.isValidIP("2001:db8:a0b:12f0::1")); //RFC 5952 compressed format.
        Assert.assertEquals(true, CommonUtil.isValidIP("2001:DB8:a0b:12f0::1")); //Upper case not allowed.


    }
}
