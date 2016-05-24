package org.wso2.carbon.gateway.httploadbalancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.Parameter;
import org.wso2.carbon.gateway.core.config.ParameterHolder;
import org.wso2.carbon.gateway.core.config.dsl.external.WUMLConfigurationBuilder;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorBuilder;

/**
 * A Class responsible for loading LB config from WUMLBaseListenerImpl.java to LoadBalancer mediator.
 */
public class LoadBalancerConfigHolder {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfigHolder.class);

    private ParameterHolder loadbalancerConfigs;

    private WUMLConfigurationBuilder.IntegrationFlow integrationFlow;

    public LoadBalancerConfigHolder() {

        this.loadbalancerConfigs = new ParameterHolder();
    }

    public ParameterHolder getLoadbalancerConfigs() {
        return loadbalancerConfigs;
    }

    public void setLoadbalancerConfigs(ParameterHolder loadbalancerConfigs) {
        this.loadbalancerConfigs = loadbalancerConfigs;
    }

    public WUMLConfigurationBuilder.IntegrationFlow getIntegrationFlow() {
        return integrationFlow;
    }

    public void setIntegrationFlow(WUMLConfigurationBuilder.IntegrationFlow integrationFlow) {
        this.integrationFlow = integrationFlow;
    }

    public void addToConfig(Parameter param) {
        loadbalancerConfigs.addParameter(param);
        //Parameter addedParam = this.getFromConfig(param.getName());
        //log.info(addedParam.getName() + " : " + addedParam.getValue());
    }

    public void removeFromConfig(String paramName) {

        loadbalancerConfigs.removeParameter(paramName);
    }

    public ParameterHolder getAllConfigs() {

        return loadbalancerConfigs;
    }

    public Parameter getFromConfig(String paramName) {

        return loadbalancerConfigs.getParameter(paramName);
    }


    public void configureLoadBalancerMediator(WUMLConfigurationBuilder.IntegrationFlow integrationFlow) {


        this.integrationFlow = integrationFlow;
        validateConfig();
        LoadBalancerMediatorBuilder.configure(this.integrationFlow.getGWConfigHolder(), loadbalancerConfigs);
    }

    /**
     * This method validates a given configuration, if anything is missing default value will be added.
     * TODO: Decide and implement validation rules, default values etc.,
     */

    public void validateConfig() {


        /**Algorithm related validations.*/

        if (this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue().
                equals(LoadBalancerConstants.ROUND_ROBIN)) {
            //TODO: for future use.
            log.info("Algorithm : " + this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue());
        } else {
            log.info("Currently this algorithm type is not supported...");
            //TODO: throw error or exception.
        }

        /**Session persistence related validations.*/
        String persistenceType = this.getFromConfig(LoadBalancerConstants.PERSISTENCE_TYPE).getValue();

        if (persistenceType.equals(LoadBalancerConstants.NO_PERSISTENCE)) {
            log.info("Persistence : " + persistenceType);
            //TODO: for future use.
        } else if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)) {
            log.info("Persistence : " + persistenceType);
            //TODO: for future use.
        } else if (persistenceType.equals(LoadBalancerConstants.LB_COOKIE)) {
            log.info("Persistence : " + persistenceType);
            if (loadbalancerConfigs.getParameter(LoadBalancerConstants.PERSISTENCE_SESSION_TIME_OUT) != null) {

                String sessionTimeout = this.getFromConfig
                        (LoadBalancerConstants.PERSISTENCE_SESSION_TIME_OUT).getValue();
                //TODO: timeout value limit check, string to long milliseconds type conversion.
                log.info("Persistence SESSION_TIME_OUT : " + sessionTimeout);
            } else {
                log.info("For LB_COOKIE session cookie time out has to be specified...");
                //TODO: Throw error or exception / log error message and load default value.

            }
        }

        /**TODO:SSL related validations.**/


        /**HealthCheck related validations.*/

        /**
         *Currently "PASSIVE_HEALTH_CHECK" is the only supported type of HealthCheck,
         * yet this check is made in case any other type is supported in future.
         */
        if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue().
                equals(LoadBalancerConstants.PASSIVE_HEALTH_CHECK)) {
            log.info("Passive health check...");
            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT) != null) {

                String hcReqTimeOut = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT).getValue();
                //TODO: timeout value limit check, string to long milliseconds type conversion.
                log.info(LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT + " : " + hcReqTimeOut);

            } else {
                log.info(LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT + " : NULL");
                //TODO: Throw error or exception / log error message and load default value.
            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES) != null) {

                String hcUHRetries = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES).getValue();
                //TODO: value limit check, string to int retries type conversion.
                log.info(LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES + " : " + hcUHRetries);

            } else {

                log.info(LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES + " : NULL");
                //TODO: Throw error or exception / log error message and load default value.


            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES) != null) {

                String hcHRetries = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES).getValue();
                //TODO: value limit check, string to int retries type conversion.
                log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES + " : " + hcHRetries);

            } else {

                //TODO: Throw error or exception / log error message and load default value.
                log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES + " : NULL");
            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL) != null) {

                String hcHCInterval = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL).getValue();
                //TODO: timeout interval limit check, string to long milliseconds type conversion.
                log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL + " : " + hcHCInterval);

            } else {

                //TODO: Throw error or exception / log error message and load default value.
                log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL + " : NULL");

            }

        }

    }


}
