package org.wso2.carbon.gateway.httploadbalancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.gateway.core.config.Parameter;
import org.wso2.carbon.gateway.core.config.ParameterHolder;
import org.wso2.carbon.gateway.core.config.dsl.external.WUMLConfigurationBuilder;
import org.wso2.carbon.gateway.core.outbound.OutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.constants.LoadBalancerConstants;
import org.wso2.carbon.gateway.httploadbalancer.context.LoadBalancerConfigContext;
import org.wso2.carbon.gateway.httploadbalancer.mediator.LoadBalancerMediatorBuilder;
import org.wso2.carbon.gateway.httploadbalancer.outbound.LBOutboundEndpoint;
import org.wso2.carbon.gateway.httploadbalancer.utils.CommonUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Class responsible for loading LB config from WUMLBaseListenerImpl.java to LoadBalancerConfigContext.
 * <p>
 * All validations and conversions are done here.
 * <p>
 * This holds static configuration from .iflow config files.
 */
public class LoadBalancerConfigHolder {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerConfigHolder.class);

    private ParameterHolder loadbalancerConfigs;

    private WUMLConfigurationBuilder.IntegrationFlow integrationFlow;

    private final LoadBalancerConfigContext context;

    /**
     * Default Constructor.
     */
    public LoadBalancerConfigHolder() {

        this.loadbalancerConfigs = new ParameterHolder();
        this.context = new LoadBalancerConfigContext();

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

    /**
     * @param param Parameter to be added to Config,
     */
    public void addToConfig(Parameter param) {
        loadbalancerConfigs.addParameter(param);
        //Parameter addedParam = this.getFromConfig(param.getName());
        //log.info(addedParam.getName() + " : " + addedParam.getValue());
    }

    /**
     * @param paramName parameterName to be removed from config.
     */
    public void removeFromConfig(String paramName) {

        loadbalancerConfigs.removeParameter(paramName);
    }

    /**
     * @return returns all configs.
     */
    public ParameterHolder getAllConfigs() {

        return loadbalancerConfigs;
    }

    /**
     * @param paramName parameterName
     * @return Parameter object corresponding to that name.
     */
    public Parameter getFromConfig(String paramName) {

        return loadbalancerConfigs.getParameter(paramName);
    }


    /**
     * @param integrationFlow integrationFlow object.
     *                        <p>
     *                        It performs validation and also initializes LoadBalancerConfigHolder.
     */
    public void configureLoadBalancerMediator(WUMLConfigurationBuilder.IntegrationFlow integrationFlow) {


        this.integrationFlow = integrationFlow;

        Set<Map.Entry<String, OutboundEndpoint>> entrySet = integrationFlow.
                getGWConfigHolder().getOutboundEndpoints().entrySet();

        /**
         * Since all OutboundEndpoint Objects MUST be accessed via LBOutboundEndpoint, we are doing this.
         * Here we are creating LBOutboundEndpoint Map similar to OutboundEndpoint Map.
         * See LBOutboundEndpoint class to understand it better.
         */
        Map<String, LBOutboundEndpoint> lbOutboundEndpointMap = new ConcurrentHashMap<>();

        for (Map.Entry entry : entrySet) {

            lbOutboundEndpointMap.put(entry.getKey().toString(),
                    new LBOutboundEndpoint((OutboundEndpoint) entry.getValue()));
        }

        context.setLbOutboundEndpoints(lbOutboundEndpointMap);

        validateConfig();

        LoadBalancerMediatorBuilder.configure(this.integrationFlow.getGWConfigHolder(), context);
    }

    /**
     * @param timeOut
     * @return boolean
     * <p>
     * This method is used to check whether scheduled is within limit or not.
     */
    private boolean isWithInLimit(int timeOut) {

        if (timeOut <= LoadBalancerConstants.MAX_TIMEOUT_VAL) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param lbOutboundEndpoints <p>
     *                            Populates cookie handling maps.
     */
    private void populateCookieMaps(Map<String, LBOutboundEndpoint> lbOutboundEndpoints) {

        //Initializing cookie maps.
        context.initCookieMaps();
        int index = 1;

        Set<Map.Entry<String, LBOutboundEndpoint>> entrySet = lbOutboundEndpoints.entrySet();
        for (Map.Entry entry : entrySet) {

            context.addToCookieToOutboundEPKeyMap(
                    LoadBalancerConstants.COOKIE_PREFIX + String.valueOf(index),
                    entry.getKey().toString());

            context.addToOutboundEPTOCookieMap(
                    CommonUtil.getHostAndPort(
                            ((LBOutboundEndpoint) entry.getValue()).getOutboundEndpoint().getUri()),
                    LoadBalancerConstants.COOKIE_PREFIX + String.valueOf(index));


            index++;
        }

    }

    /**
     * Algorithm related validations.
     */

    private void validateAlgorithm() {

        if (this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue().
                equals(LoadBalancerConstants.ROUND_ROBIN)) {

            context.setAlgorithm(this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue());
            log.info("Algorithm : " + context.getAlgorithm());

        } else if (this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue().
                equals(LoadBalancerConstants.STRICT_IP_HASHING)) {

            context.setAlgorithm(this.getFromConfig(LoadBalancerConstants.ALGORITHM_NAME).getValue());
            log.info("Algorithm : " + context.getAlgorithm());
        } else {
            log.error("Currently this algorithm type is not supported...");

        }

    }

    /**
     * Session persistence related validations.
     */

    private void validatePersistence() {

        String persistenceType = this.getFromConfig(LoadBalancerConstants.PERSISTENCE_TYPE).getValue();

        if (persistenceType.equals(LoadBalancerConstants.NO_PERSISTENCE)) {

            context.setPersistence(persistenceType);
            log.info("Persistence : " + context.getPersistence());

        } else if (persistenceType.equals(LoadBalancerConstants.APPLICATION_COOKIE)) {

            context.setPersistence(persistenceType);
            log.info("Persistence : " + context.getPersistence());
            populateCookieMaps(context.getLbOutboundEndpoints());


        } else if (persistenceType.equals(LoadBalancerConstants.LB_COOKIE)) {

            //TODO: Populate cookie map.
            context.setPersistence(persistenceType);
            log.info("Persistence : " + context.getPersistence());

            /** TODO: Discuss this.
             if (loadbalancerConfigs.getParameter(LoadBalancerConstants.PERSISTENCE_SESSION_TIME_OUT) != null) {

             String sessionTimeout = this.getFromConfig
             (LoadBalancerConstants.PERSISTENCE_SESSION_TIME_OUT).getValue();

             int sessTimeout = CommonUtil.getTimeInMilliSeconds(sessionTimeout);

             if (isWithInLimit(sessTimeout)) {

             context.setSessionPersistenceTimeout(sessTimeout);
             log.info("Persistence Timeout : " + context.getSessionPersistenceTimeout());
             } else {
             //TODO: Is this okay..?
             context.setSessionPersistenceTimeout(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);
             log.error("Value greater than Max limit. Loading default value...Persistence Timeout :  " +
             context.getSessionPersistenceTimeout());
             }


             } else {

             log.info("For LB_COOKIE session cookie time out has to be specified...");
             //TODO: Is this okay..?
             context.setSessionPersistenceTimeout(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);
             log.error("For LB_COOKIE session cookie time out has to be specified.. Loading default value..." +
             "Persistence Timeout :  " + context.getSessionPersistenceTimeout());

             }**/
            populateCookieMaps(context.getLbOutboundEndpoints());
        } else if (persistenceType.equals(LoadBalancerConstants.CLIENT_IP_HASHING)) {

            context.setPersistence(persistenceType);
            log.info("Persistence : " + context.getPersistence());
        }

    }

    /**
     * SSL related validations.
     **/

    private void validateSSL() {

        if (this.getFromConfig(LoadBalancerConstants.SSL_TYPE).getValue().
                equals(LoadBalancerConstants.NO_SSL)) {

            context.setSslType(this.getFromConfig(LoadBalancerConstants.SSL_TYPE).getValue());
            log.info("SSL Support : " + context.getSslType());

        } else {

            log.info("Currently this type of SSL is not supported..");

        }

    }

    /**
     * HealthCheck related validations.
     */

    private void validateHealthCheck() {

        /**
         *For PASSIVE_HEALTH_CHECK.
         */
        if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue().
                equals(LoadBalancerConstants.PASSIVE_HEALTH_CHECK)) {

            context.setHealthCheck(this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue());
            log.info("HEALTH CHECK TYPE : " + context.getHealthCheck());

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT) != null) {

                String hcReqTimeOut = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_REQUEST_TIMEOUT).getValue();

                int timeout = CommonUtil.getTimeInMilliSeconds(hcReqTimeOut);

                if (isWithInLimit(timeout)) {
                    context.setReqTimeout(timeout);
                    log.info("Request TIME_OUT : " + context.getReqTimeout());
                } else {
                    //TODO: Is this okay..?
                    context.setReqTimeout(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);
                    log.error("Exceeded TIMEOUT LIMIT. Loading DEFAULT value for " +
                            "Request TIME_OUT : " + context.getReqTimeout());
                }


            } else {
                //TODO: Is this okay..?
                context.setReqTimeout(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);
                log.error("LB_REQUEST_TIMEOUT NOT SPECIFIED. Loading DEFAULT value for " +
                        "Request TIME_OUT : " + context.getReqTimeout());
            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES) != null) {

                String hcUHRetries = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES).getValue();


                int uhRetries = CommonUtil.getRetriesCount(hcUHRetries);
                context.setUnHealthyRetries(uhRetries);
                log.info(LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES + " : " + context.getUnHealthyRetries());

            } else {
                //TODO: Is this okay..?
                context.setUnHealthyRetries(LoadBalancerConstants.DEFAULT_RETRIES);
                log.error("UNHEALTHY_RETRIES_VALUE NOT SPECIFIED.. Loading default value." +
                        LoadBalancerConstants.HEALTH_CHECK_UNHEALTHY_RETRIES + " : " +
                        context.getUnHealthyRetries());

            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES) != null) {

                String hcHRetries = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES).getValue();

                int hRetries = CommonUtil.getRetriesCount(hcHRetries);
                context.setHealthyRetries(hRetries);
                log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES + " : " + context.getHealthyRetries());

            } else {
                //TODO: Is this okay..?
                context.setHealthyRetries(LoadBalancerConstants.DEFAULT_RETRIES);
                log.error("HEALTHY_RETRIES_VALUE NOT SPECIFIED.. Loading default value." +
                        LoadBalancerConstants.HEALTH_CHECK_HEALTHY_RETRIES + " : " + context.getHealthyRetries());
            }

            if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL) != null) {

                String hcHCInterval = this.getFromConfig
                        (LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL).getValue();

                int interval = CommonUtil.getTimeInMilliSeconds(hcHCInterval);

                if (isWithInLimit(interval)) {

                    context.setHealthycheckInterval(interval);
                    log.info(LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL + " : " +
                            context.getHealthycheckInterval());

                } else {
                    //TODO: Is this okay..?

                    context.setHealthycheckInterval(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);

                    log.error("Exceeded HEALTHY_CHECK_TIMEOUT LIMIT. Loading DEFAULT value for " +
                            LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL + " : " +
                            context.getHealthycheckInterval());
                }


            } else {
                //TODO: Is this okay..?

                context.setHealthycheckInterval(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);

                log.error("HEALTHY_CHECK_TIMEOUT LIMIT NOT SPECIFIED. Loading DEFAULT value for " +
                        LoadBalancerConstants.HEALTH_CHECK_HEALTHY_CHECK_INTERVAL + " : " +
                        context.getHealthycheckInterval());

            }

            validateTestRequest();

            //For DEFAULT_HEALTH_CHECK
        } else if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue().
                equals(LoadBalancerConstants.DEFAULT_HEALTH_CHECK)) {

            context.setHealthCheck(this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue());

            log.info("HEALTH CHECK TYPE : " + context.getHealthCheck());

            context.setUnHealthyRetries(LoadBalancerConstants.DEFAULT_RETRIES);
            context.setHealthyRetries(LoadBalancerConstants.DEFAULT_RETRIES);
            context.setReqTimeout(LoadBalancerConstants.DEFAULT_REQ_TIMEOUT);
            context.setHealthycheckInterval(LoadBalancerConstants.DEFAULT_HEALTHY_CHECK_INTERVAL);
            validateTestRequest();

            //FOR NO_HEALTH_CHECK
        } else if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue().
                equals(LoadBalancerConstants.NO_HEALTH_CHECK)) {

            context.setHealthCheck(this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TYPE).getValue());

            log.info("HEALTH CHECK TYPE : " + context.getHealthCheck());

        }

    }

    private void validateTestRequest() {

        if (this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TEST_REQUEST) != null) {

            this.context.setTestRequest(
                    this.getFromConfig(LoadBalancerConstants.HEALTH_CHECK_TEST_REQUEST).getValue());
            log.info("TestRequest : " + context.getTestRequest());

        } else {

            log.info("TestRequest : " + context.getTestRequest());
            log.error("Health Check is enabled, but no testRequest is provided. LB will try only to make" +
                    "Inet Socket connection to check timeOut. ");

        }

    }


    /**
     * This method validates a given configuration, if anything is missing default value will be added.
     * TODO: check default values limit.
     */

    private void validateConfig() {

        validateAlgorithm();
        validatePersistence();
        validateSSL();
        validateHealthCheck();

    }

}
