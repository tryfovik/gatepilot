package com.dt.platform.gateway.infrastructure.governance;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sentinel 网关规则装载器测试。
 */
class GatewaySentinelRuleRegistrarTest {

    @Test
    void shouldBuildSentinelDefinitionsFromRoutePolicies() {
        GatewayProperties properties = createGatewayProperties();
        GatewaySentinelRuleRegistrar registrar = new GatewaySentinelRuleRegistrar(
                properties,
                new GatewayRouteDefinitionLocator(properties)
        );

        Set<ApiDefinition> apiDefinitions = registrar.buildApiDefinitions();
        Set<GatewayFlowRule> flowRules = registrar.buildFlowRules();

        assertThat(apiDefinitions).hasSize(1);
        ApiDefinition apiDefinition = apiDefinitions.iterator().next();
        assertThat(apiDefinition.getApiName()).isEqualTo("platform-gateway-api-game-admin");
        assertThat(apiDefinition.getPredicateItems()).hasSize(2);

        assertThat(flowRules).hasSize(1);
        GatewayFlowRule flowRule = flowRules.iterator().next();
        assertThat(flowRule.getResource()).isEqualTo("platform-gateway-api-game-admin");
        assertThat(flowRule.getResourceMode()).isEqualTo(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME);
        assertThat(flowRule.getGrade()).isEqualTo(RuleConstant.FLOW_GRADE_QPS);
        assertThat(flowRule.getCount()).isEqualTo(120D);
        assertThat(flowRule.getIntervalSec()).isEqualTo(1L);
        assertThat(flowRule.getBurst()).isEqualTo(20);
        assertThat(flowRule.getControlBehavior()).isEqualTo(RuleConstant.CONTROL_BEHAVIOR_RATE_LIMITER);
        assertThat(flowRule.getMaxQueueingTimeoutMs()).isEqualTo(300);
        assertThat(flowRule.getParamItem()).isNotNull();
        assertThat(flowRule.getParamItem().getParseStrategy()).isEqualTo(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
        assertThat(flowRule.getParamItem().getFieldName()).isEqualTo("X-Tenant-Id");
        assertThat(flowRule.getParamItem().getPattern()).isEqualTo("vip-.*");
        assertThat(flowRule.getParamItem().getMatchStrategy()).isEqualTo(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_REGEX);
    }

    @Test
    void shouldIgnoreRoutesWithoutFlowControlPolicy() {
        GatewayProperties properties = createGatewayProperties();
        properties.getProjects().get("game").getRoutes().get("admin").getGovernance().getFlowControl().setEnabled(false);
        GatewaySentinelRuleRegistrar registrar = new GatewaySentinelRuleRegistrar(
                properties,
                new GatewayRouteDefinitionLocator(properties)
        );

        assertThat(registrar.buildApiDefinitions()).isEmpty();
        assertThat(registrar.buildFlowRules()).isEmpty();
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties gameProject = new GatewayProperties.ProjectProperties();
        gameProject.setPathSegment("game");

        GatewayProperties.RouteProperties adminRoute = new GatewayProperties.RouteProperties();
        adminRoute.setPathSegment("admin");
        adminRoute.setLegacyPathSegments(java.util.List.of("admin"));
        adminRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setServicePathPrefix("/admin");
        adminRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.getGovernance().getFlowControl().setEnabled(true);
        adminRoute.getGovernance().getFlowControl().setCount(120D);
        adminRoute.getGovernance().getFlowControl().setBurst(20);
        adminRoute.getGovernance().getFlowControl().setControlBehavior("rate-limiter");
        adminRoute.getGovernance().getFlowControl().setMaxQueueingTimeoutMs(300);
        adminRoute.getGovernance().getFlowControl().getParam().setEnabled(true);
        adminRoute.getGovernance().getFlowControl().getParam().setParseStrategy("header");
        adminRoute.getGovernance().getFlowControl().getParam().setFieldName("X-Tenant-Id");
        adminRoute.getGovernance().getFlowControl().getParam().setPattern("vip-.*");
        adminRoute.getGovernance().getFlowControl().getParam().setMatchStrategy("regex");

        GatewayProperties.RouteProperties openRoute = new GatewayProperties.RouteProperties();
        openRoute.setPathSegment("open");
        openRoute.setLegacyPathSegments(java.util.List.of("open"));
        openRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        openRoute.setServicePathPrefix("/open");
        openRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));

        gameProject.getRoutes().put("admin", adminRoute);
        gameProject.getRoutes().put("open", openRoute);
        properties.getProjects().put("game", gameProject);
        return properties;
    }
}
