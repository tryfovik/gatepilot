package com.dt.gatepilot.proxy.infrastructure.governance;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sentinel 网关规则发布器测试。
 */
class SentinelGatewayRulePublisherTest {

    @Test
    void shouldBuildSentinelRulesFromPublishedRateLimitPolicy() {
        SentinelGatewayRulePublisher publisher = new SentinelGatewayRulePublisher(new RateLimitPolicyResolver());
        CompiledProxyRuntime runtime = new PublishedConfigCompiler().compile(config());

        Set<ApiDefinition> apiDefinitions = publisher.buildApiDefinitions(runtime);
        Set<GatewayFlowRule> flowRules = publisher.buildFlowRules(runtime);

        assertThat(apiDefinitions).hasSize(1);
        ApiDefinition apiDefinition = apiDefinitions.iterator().next();
        assertThat(apiDefinition.getApiName()).isEqualTo("gatepilot-api-game-admin");
        assertThat(apiDefinition.getPredicateItems()).hasSize(1);

        assertThat(flowRules).hasSize(2);
        assertThat(flowRules)
                .extracting(GatewayFlowRule::getResource)
                .containsOnly("gatepilot-api-game-admin");
        assertThat(flowRules)
                .extracting(GatewayFlowRule::getResourceMode)
                .containsOnly(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME);
        assertThat(flowRules)
                .extracting(GatewayFlowRule::getGrade)
                .containsOnly(RuleConstant.FLOW_GRADE_QPS);
        assertThat(flowRules)
                .extracting(GatewayFlowRule::getCount)
                .contains(120D, 50D);
        GatewayFlowRule paramRule = flowRules.stream()
                .filter(rule -> rule.getParamItem() != null)
                .findFirst()
                .orElseThrow();
        assertThat(paramRule.getParamItem().getParseStrategy())
                .isEqualTo(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER);
        assertThat(paramRule.getParamItem().getFieldName()).isEqualTo("X-Tenant-Id");
        assertThat(paramRule.getParamItem().getPattern()).isEqualTo("vip");
        assertThat(paramRule.getParamItem().getMatchStrategy())
                .isEqualTo(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT);
    }

    @Test
    void shouldIgnoreRouteWithoutRateLimitPolicy() {
        SentinelGatewayRulePublisher publisher = new SentinelGatewayRulePublisher(new RateLimitPolicyResolver());
        PublishedConfig config = config();
        config.getSpec().getRoutes().get(0).getPolicyNames().clear();
        CompiledProxyRuntime runtime = new PublishedConfigCompiler().compile(config);

        assertThat(publisher.buildApiDefinitions(runtime)).isEmpty();
        assertThat(publisher.buildFlowRules(runtime)).isEmpty();
    }

    private PublishedConfig config() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("game-admin");
        route.setPath("/api/game/admin");
        route.setUpstreamName("game-upstream");
        route.getPolicyNames().add("traffic");
        config.getSpec().getRoutes().add(route);

        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic");
        policy.setType(PublishedConfigConstants.POLICY_TYPE_TRAFFIC);
        policy.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, rateLimit());
        config.getSpec().getPolicies().add(policy);
        return config;
    }

    private TrafficPolicy.RateLimitPolicy rateLimit() {
        TrafficPolicy.RateLimitPolicy rateLimit = new TrafficPolicy.RateLimitPolicy();
        rateLimit.setEnabled(true);
        rateLimit.setRequestsPerSecond(120);
        TrafficPolicy.ParamLimitRule paramRule = new TrafficPolicy.ParamLimitRule();
        paramRule.setSource(ProxyRateLimitConstants.SOURCE_HEADER);
        paramRule.setName("X-Tenant-Id");
        paramRule.setValue("vip");
        paramRule.setRequestsPerSecond(50);
        rateLimit.getParamRules().add(paramRule);
        return rateLimit;
    }
}
