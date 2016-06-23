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
    }
}
