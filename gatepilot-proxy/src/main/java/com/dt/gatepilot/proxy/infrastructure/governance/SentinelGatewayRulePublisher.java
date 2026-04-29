/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.proxy.infrastructure.governance;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayParamFlowItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.dt.gatepilot.proxy.domain.port.RuntimeGovernanceRulePublisher;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitPolicy;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRoute;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Sentinel 网关规则发布器。
 */
public class SentinelGatewayRulePublisher implements RuntimeGovernanceRulePublisher {

    private static final Logger log = LoggerFactory.getLogger(SentinelGatewayRulePublisher.class);

    /**
     * 限流策略解析器。
     */
    private final RateLimitPolicyResolver rateLimitPolicyResolver;

    /**
     * 创建 Sentinel 网关规则发布器。
     *
     * @param rateLimitPolicyResolver 限流策略解析器
     */
    public SentinelGatewayRulePublisher(RateLimitPolicyResolver rateLimitPolicyResolver) {
        this.rateLimitPolicyResolver = rateLimitPolicyResolver;
    }

    /**
     * 发布运行态治理规则。
     *
     * @param runtime 已编译运行态
     */
    @Override
    public void publish(CompiledProxyRuntime runtime) {
        Set<ApiDefinition> apiDefinitions = buildApiDefinitions(runtime);
        Set<GatewayFlowRule> flowRules = buildFlowRules(runtime);
        // Sentinel manager 会整体替换本机规则
        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);
        GatewayRuleManager.loadRules(flowRules);
        log.info("Loaded {} Sentinel gateway api definitions and {} flow rules for GatePilot proxy",
                apiDefinitions.size(), flowRules.size());
    }

    /**
     * 构建 Sentinel API 定义。
     *
     * @param runtime 已编译运行态
     * @return API 定义集合
     */
    Set<ApiDefinition> buildApiDefinitions(CompiledProxyRuntime runtime) {
        Set<ApiDefinition> apiDefinitions = new LinkedHashSet<>();
        if (runtime == null) {
            return apiDefinitions;
        }
        for (CompiledRoute route : runtime.getRoutes()) {
            // 只有启用限流的路由才需要 Sentinel API 定义
            Optional<CompiledRateLimitPolicy> policy = rateLimitPolicyResolver.resolve(runtime, route);
            if (policy.isEmpty() || !StringUtils.hasText(route.getPathPrefix())) {
                continue;
            }
            Set<ApiPredicateItem> predicates = new LinkedHashSet<>();
            predicates.add(new ApiPathPredicateItem()
                    .setPattern(route.getPathPrefix())
                    .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX));
            apiDefinitions.add(new ApiDefinition(apiName(route)).setPredicateItems(predicates));
        }
        return apiDefinitions;
    }

    /**
     * 构建 Sentinel 流控规则。
     *
     * @param runtime 已编译运行态
     * @return 流控规则集合
     */
    Set<GatewayFlowRule> buildFlowRules(CompiledProxyRuntime runtime) {
        Set<GatewayFlowRule> flowRules = new LinkedHashSet<>();
        if (runtime == null) {
            return flowRules;
        }
        for (CompiledRoute route : runtime.getRoutes()) {
            // Sentinel 规则从已发布限流策略派生，不读取草稿配置
            Optional<CompiledRateLimitPolicy> policy = rateLimitPolicyResolver.resolve(runtime, route);
            if (policy.isEmpty()) {
                continue;
            }
            for (CompiledRateLimitRule rule : policy.get().getRules()) {
                GatewayFlowRule flowRule = baseFlowRule(route, rule);
                if (StringUtils.hasText(rule.getSource())) {
                    flowRule.setParamItem(paramItem(rule));
                }
                flowRules.add(flowRule);
            }
        }
        return flowRules;
    }

    private GatewayFlowRule baseFlowRule(CompiledRoute route, CompiledRateLimitRule rule) {
        // 当前 GatePilot 限流模型按 QPS 映射 Sentinel gateway rule
        GatewayFlowRule flowRule = new GatewayFlowRule(apiName(route))
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)
                .setCount(rule.getRequestsPerSecond())
                .setIntervalSec(ProxySentinelConstants.DEFAULT_INTERVAL_SECONDS)
                .setBurst(ProxySentinelConstants.DEFAULT_BURST)
                .setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT)
                .setMaxQueueingTimeoutMs(ProxySentinelConstants.DEFAULT_MAX_QUEUEING_TIMEOUT_MILLIS);
        return flowRule;
    }

    private GatewayParamFlowItem paramItem(CompiledRateLimitRule rule) {
        // 参数规则只映射 GatePilot 当前支持的精确匹配
        GatewayParamFlowItem item = new GatewayParamFlowItem()
                .setParseStrategy(parseStrategy(rule.getSource()))
                .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_EXACT);
        if (StringUtils.hasText(rule.getName())) {
            item.setFieldName(rule.getName().trim());
        }
        if (StringUtils.hasText(rule.getValue())) {
            item.setPattern(rule.getValue().trim());
        }
        return item;
    }

    private int parseStrategy(String source) {
        // source 名称沿用 GatePilot 限流模型
        String normalized = source == null ? null : source.trim().toLowerCase(Locale.ROOT);
        if (ProxyRateLimitConstants.SOURCE_HOST.equals(normalized)) {
            return SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HOST;
        }
        if (ProxyRateLimitConstants.SOURCE_HEADER.equals(normalized)) {
            return SentinelGatewayConstants.PARAM_PARSE_STRATEGY_HEADER;
        }
        if (ProxyRateLimitConstants.SOURCE_QUERY.equals(normalized)
                || ProxyRateLimitConstants.SOURCE_URL_PARAM.equals(normalized)) {
            return SentinelGatewayConstants.PARAM_PARSE_STRATEGY_URL_PARAM;
        }
        if (ProxyRateLimitConstants.SOURCE_COOKIE.equals(normalized)) {
            return SentinelGatewayConstants.PARAM_PARSE_STRATEGY_COOKIE;
        }
        return SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP;
    }

    private String apiName(CompiledRoute route) {
        // 加前缀避免和其他 Sentinel gateway API 名称冲突
        return ProxySentinelConstants.API_NAME_PREFIX + route.getRouteId();
    }
}
