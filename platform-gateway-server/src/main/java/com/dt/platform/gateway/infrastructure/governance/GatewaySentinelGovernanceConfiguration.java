package com.dt.platform.gateway.infrastructure.governance;

import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 平台网关 Sentinel 治理装配。
 */
@Configuration
@ConditionalOnClass({GatewayRuleManager.class, GatewayApiDefinitionManager.class})
@ConditionalOnProperty(
        prefix = "getboot.governance",
        name = {"enabled", "sentinel.enabled"},
        havingValue = "true"
)
public class GatewaySentinelGovernanceConfiguration {

    /**
     * 注册 Sentinel 网关规则装载器。
     *
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     * @return 规则装载器
     */
    @Bean
    public GatewaySentinelRuleRegistrar gatewaySentinelRuleRegistrar(GatewayProperties properties,
                                                                     GatewayRouteDefinitionLocator routeDefinitionLocator) {
        return new GatewaySentinelRuleRegistrar(properties, routeDefinitionLocator);
    }
}
