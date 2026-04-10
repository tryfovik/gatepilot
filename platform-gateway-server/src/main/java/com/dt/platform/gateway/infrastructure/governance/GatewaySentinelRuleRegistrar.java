package com.dt.platform.gateway.infrastructure.governance;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 根据平台网关配置装载 Sentinel 网关规则。
 */
public class GatewaySentinelRuleRegistrar implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(GatewaySentinelRuleRegistrar.class);

    private final GatewayProperties properties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    /**
     * 创建 Sentinel 规则装载器。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public GatewaySentinelRuleRegistrar(GatewayProperties properties,
                                        GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.properties = properties;
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @Override
    public void afterSingletonsInstantiated() {
        register();
    }

    void register() {
        Set<ApiDefinition> apiDefinitions = buildApiDefinitions();
        Set<GatewayFlowRule> flowRules = buildFlowRules();
        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);
        GatewayRuleManager.loadRules(flowRules);
        log.info("Loaded {} Sentinel gateway api definitions and {} flow rules for platform gateway",
                apiDefinitions.size(), flowRules.size());
    }

    Set<ApiDefinition> buildApiDefinitions() {
        Set<ApiDefinition> apiDefinitions = new LinkedHashSet<>();
        for (GatewayRouteDefinition definition : routeDefinitionLocator.getRouteDefinitions()) {
            GatewayProperties.RouteProperties route = getRouteProperties(definition);
            if (!route.getGovernance().getFlowControl().isEnabled() || definition.getApiPathRoots().isEmpty()) {
                continue;
            }
            Set<com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem> predicates = new LinkedHashSet<>();
            for (String apiPathRoot : definition.getApiPathRoots()) {
                predicates.add(new ApiPathPredicateItem()
                        .setPattern(apiPathRoot)
                        .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
            }
            apiDefinitions.add(new ApiDefinition(definition.buildGovernanceApiName())
                    .setPredicateItems(predicates));
        }
        return apiDefinitions;
    }

    Set<GatewayFlowRule> buildFlowRules() {
        Set<GatewayFlowRule> flowRules = new LinkedHashSet<>();
        for (GatewayRouteDefinition definition : routeDefinitionLocator.getRouteDefinitions()) {
            GatewayProperties.RouteProperties route = getRouteProperties(definition);
            GatewayProperties.FlowControlProperties flowControl = route.getGovernance().getFlowControl();
            if (!flowControl.isEnabled()) {
                continue;
            }
            GatewayFlowRule rule = new GatewayFlowRule(definition.buildGovernanceApiName())
                    .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                    .setGrade(RuleConstant.FLOW_GRADE_QPS)
                    .setCount(flowControl.getCount())
                    .setIntervalSec(flowControl.getIntervalSec())
                    .setBurst(flowControl.getBurst())
                    .setControlBehavior(mapControlBehavior(flowControl.getControlBehavior()))
                    .setMaxQueueingTimeoutMs(flowControl.getMaxQueueingTimeoutMs());
            if (flowControl.getParam().isEnabled()) {
                rule.setParamItem(buildParamItem(flowControl.getParam()));
            }
            flowRules.add(rule);
        }
        return flowRules;
    }

    private GatewayParamFlowItem buildParamItem(GatewayProperties.FlowControlParamProperties param) {
        GatewayParamFlowItem item = new GatewayParamFlowItem()
                .setParseStrategy(mapParseStrategy(param.getParseStrategy()))
                .setMatchStrategy(mapMatchStrategy(param.getMatchStrategy()));
        if (StringUtils.hasText(param.getFieldName())) {
            item.setFieldName(param.getFieldName().trim());
        }
        if (StringUtils.hasText(param.getPattern())) {
            item.setPattern(param.getPattern().trim());
        }
        return item;
    }

    private int mapControlBehavior(String controlBehavior) {
        return switch (normalizeLiteral(controlBehavior)) {
            case "warm-up" -> RuleConstant.CONTROL_BEHAVIOR_WARM_UP;
            case "rate-limiter" -> RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER;
            case "warm-up-rate-limiter" -> RuleConstant.CONTROL_BEHAVIOR_WARM_UP_RATE_LIMITER;
            default -> RuleConstant.CONTROL_BEHAVIOR_DEFAULT;
        };
    }

    private int mapParseStrategy(String parseStrategy) {
        return switch (normalizeLiteral(parseStrategy)) {
            case "host" -> SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HOST;
            case "header" -> SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER;
            case "url-param" -> SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM;
            case "cookie" -> SentinelGatewayConstants.PARAM_PARSE_STRATEGY_COOKIE;
            default -> SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP;
        };
    }

    private int mapMatchStrategy(String matchStrategy) {
        return switch (normalizeLiteral(matchStrategy)) {
            case "prefix" -> SentinelGatewayConstants.PARAM_MATCH_STRATEGY_PREFIX;
            case "regex" -> SentinelGatewayConstants.PARAM_MATCH_STRATEGY_REGEX;
            case "contains" -> SentinelGatewayConstants.PARAM_MATCH_STRATEGY_CONTAINS;
            default -> SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT;
        };
    }

    private String normalizeLiteral(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private GatewayProperties.RouteProperties getRouteProperties(GatewayRouteDefinition definition) {
        GatewayProperties.ProjectProperties project = properties.getProjects().get(definition.getProjectKey());
        if (project == null) {
            throw new IllegalStateException("gateway project properties not found: " + definition.getProjectKey());
        }
        GatewayProperties.RouteProperties route = project.getRoutes().get(definition.getRouteKey());
        if (route == null) {
            throw new IllegalStateException("gateway route properties not found: "
                    + definition.getProjectKey() + "/" + definition.getRouteKey());
        }
        return route;
    }
}
