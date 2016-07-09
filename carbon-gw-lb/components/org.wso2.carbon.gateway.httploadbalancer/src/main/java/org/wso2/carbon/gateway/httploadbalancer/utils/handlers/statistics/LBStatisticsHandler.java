package org.wso2.carbon.gateway.httploadbalancer.utils.handlers.statistics;

import org.wso2.carbon.transport.http.netty.statistics.StatisticsHandler;
import org.wso2.carbon.transport.http.netty.statistics.TimerHolder;

/**
 * Holds StatisticsHandler instance for LB.
 */
public final class LBStatisticsHandler {
    private static final StatisticsHandler STATISTICS_HANDLER = new StatisticsHandler(TimerHolder.getInstance());

    public static StatisticsHandler getStatisticsHandler() {
        return STATISTICS_HANDLER;
    }

    private LBStatisticsHandler() {

    }
}
