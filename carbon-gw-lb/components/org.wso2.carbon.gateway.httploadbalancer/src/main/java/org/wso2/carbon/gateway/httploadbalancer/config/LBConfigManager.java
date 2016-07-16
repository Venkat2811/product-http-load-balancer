package org.wso2.carbon.gateway.httploadbalancer.config;

import org.wso2.carbon.gateway.core.config.dsl.external.WUMLConfigurationBuilder;


/**
 * This is responsible to create group and non group LBMediators.
 */
public class LBConfigManager {

    public static void configureLoadBalancer(WUMLConfigurationBuilder.IntegrationFlow integrationFlow,
                                             LoadBalancerConfigHolder lbConfigHolder) {

        lbConfigHolder.configureLoadBalancerMediator(integrationFlow);

    }

    public static void configureGroupLoadBalancer(WUMLConfigurationBuilder.IntegrationFlow integrationFlow,
                                                  LoadBalancerConfigHolder lbConfigHolder, String groupPath) {

        lbConfigHolder.configureGroupLoadbalancerMediator(integrationFlow, groupPath);

    }
}
