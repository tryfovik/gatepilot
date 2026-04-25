package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.traffic.GatewayTrafficColorFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;

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

    @Test
    void shouldResolveTrafficColorFromWeightedReleaseVariant() {
        GatewayProperties properties = createWeightedReleaseGatewayProperties();
        GatewayTrafficColorFilter filter = new GatewayTrafficColorFilter(
                properties.getTrafficColor(),
                new GatewayRouteDefinitionLocator(properties)
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/admin/system/ping")
                        .header("X-User-Id", "alice")
        );

        filter.filter(exchange, mutatedExchange -> {
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("green");
            return mutatedExchange.getResponse().setComplete();
        }).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("green");
    }

    @Test
    void shouldFallbackToDefaultColorWhenWeightedReleaseVariantNotSelected() {
        GatewayProperties properties = createWeightedReleaseGatewayProperties();
        GatewayTrafficColorFilter filter = new GatewayTrafficColorFilter(
                properties.getTrafficColor(),
                new GatewayRouteDefinitionLocator(properties)
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/admin/system/ping")
                        .header("X-User-Id", "bob")
        );

        filter.filter(exchange, mutatedExchange -> {
            assertThat(mutatedExchange.getRequest().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("stable");
            return mutatedExchange.getResponse().setComplete();
        }).block();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-Traffic-Color")).isEqualTo("stable");
    }

    private GatewayProperties createWeightedReleaseGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        properties.getTrafficColor().setEnabled(true);

        GatewayProperties.ProjectProperties project = new GatewayProperties.ProjectProperties();
        project.setPathSegment("game");

        GatewayProperties.RouteProperties route = new GatewayProperties.RouteProperties();
        route.setPathSegment("admin");
        route.setServiceUri(URI.create("http://127.0.0.1:18080"));
        route.setServicePathPrefix("/admin");
        route.setActuatorUri(URI.create("http://127.0.0.1:18080"));

        GatewayProperties.ReleaseVariantProperties greenVariant = new GatewayProperties.ReleaseVariantProperties();
        greenVariant.setWeight(30);
        route.getRelease().getVariants().put("green", greenVariant);

        project.getRoutes().put("admin", route);
        properties.getProjects().put("game", project);
        return properties;
    }
}
