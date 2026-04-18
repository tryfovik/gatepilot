package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.traffic.GatewayTrafficColorFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流量染色过滤器测试。
 */
class GatewayTrafficColorFilterTest {

    @Test
    void shouldResolveTrafficColorFromConfiguredRule() {
        GatewayProperties.TrafficColorProperties trafficColor = new GatewayProperties.TrafficColorProperties();
        trafficColor.setEnabled(true);
        GatewayProperties.TrafficColorRuleProperties rule = new GatewayProperties.TrafficColorRuleProperties();
        rule.setSource("header");
        rule.setFieldName("X-Canary");
        rule.setPattern("true");
        rule.setColor("green");
        trafficColor.setRules(java.util.List.of(rule));

        GatewayTrafficColorFilter filter = new GatewayTrafficColorFilter(trafficColor);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/open/system/ping")
                        .header("X-Canary", "true")
        );

        filter.filter(exchange, mutatedExchange -> {
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("green");
            assertThat((String) mutatedExchange.getAttribute(GatewayTrafficColorFilter.TRAFFIC_COLOR_ATTRIBUTE))
                    .isEqualTo("green");
            return mutatedExchange.getResponse().setComplete();
        }).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("green");
    }

    @Test
    void shouldTrustIncomingTrafficColorWhenConfigured() {
        GatewayProperties.TrafficColorProperties trafficColor = new GatewayProperties.TrafficColorProperties();
        trafficColor.setEnabled(true);
        trafficColor.setTrustRequestHeader(true);

        GatewayTrafficColorFilter filter = new GatewayTrafficColorFilter(trafficColor);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/open/system/ping")
                        .header("X-Traffic-Color", "blue")
        );

        filter.filter(exchange, mutatedExchange -> {
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("blue");
            return mutatedExchange.getResponse().setComplete();
        }).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("blue");
    }
}
