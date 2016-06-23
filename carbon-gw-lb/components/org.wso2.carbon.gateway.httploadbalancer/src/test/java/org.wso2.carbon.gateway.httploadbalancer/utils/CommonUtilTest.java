package org.wso2.carbon.gateway.httploadbalancer.utils;

import org.junit.Assert;
import org.junit.Test;


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
        Assert.assertEquals(null, CommonUtil.getHostAndPort("http://localhost:8080/stockquote\\all"));
        Assert.assertEquals(null, CommonUtil.getHostAndPort("htp:://localhost:8080/stockquote/all"));
    }
}
