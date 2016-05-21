package org.wso2.carbon.gateway.httploadbalancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.GWConfigHolder;
import org.wso2.carbon.gateway.core.config.Parameter;
import org.wso2.carbon.gateway.core.config.ParameterHolder;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorBuilder;

/**
 * A Class responsible for loading LB config from WUMLBaseListenerImpl.java to LoadBalancer mediator.
 */
public class LoadBalancerConfigHolder {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfigHolder.class);

    private ParameterHolder loadbalancerConfigs;

    private GWConfigHolder gwConfigHolder;

    public LoadBalancerConfigHolder() {

        this.loadbalancerConfigs = new ParameterHolder();
    }

    public ParameterHolder getLoadbalancerConfigs() {
        return loadbalancerConfigs;
    }

    public void setLoadbalancerConfigs(ParameterHolder loadbalancerConfigs) {
        this.loadbalancerConfigs = loadbalancerConfigs;
    }

    public GWConfigHolder getGwConfigHolder() {
        return gwConfigHolder;
    }

    public void setGwConfigHolder(GWConfigHolder gwConfigHolder) {
        this.gwConfigHolder = gwConfigHolder;
    }

    public void addToConfig(Parameter param) {
        loadbalancerConfigs.addParameter(param);
        Parameter addedParam = getFromConfig(param.getName());
        log.info(addedParam.getName() + " : " + addedParam.getValue());
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


    public void configureLoadBalancerMediator(GWConfigHolder gwConfigHolder) {

        this.gwConfigHolder = gwConfigHolder;
        LoadBalancerMediatorBuilder.configure(gwConfigHolder, loadbalancerConfigs);
    }

    /**
     * This method validates a given configuration, if anything is missing default value will be added.
     * TODO: Decide and implement validation rules, default values etc.,
     */
    public void validateConfig() {

    }


}
