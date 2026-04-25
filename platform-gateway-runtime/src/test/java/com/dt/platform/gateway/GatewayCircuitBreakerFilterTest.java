package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.governance.GatewayCircuitBreakerFilter;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.WebFilterChain;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由级熔断过滤器测试。
 */
class GatewayCircuitBreakerFilterTest {

    @Test
    void shouldOpenCircuitAndReturnFallbackAfterFailureThresholdReached() {
        GatewayCircuitBreakerFilter filter = createFilter();
        AtomicInteger chainCalls = new AtomicInteger();
        WebFilterChain failingChain = exchange -> {
            chainCalls.incrementAndGet();
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        };

        filter.filter(exchange(), failingChain).block();
        MockServerWebExchange fallbackExchange = exchange();

        filter.filter(fallbackExchange, failingChain).block();

        assertThat(chainCalls).hasValue(1);
        assertThat(fallbackExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(fallbackExchange.getResponse().getBodyAsString().block())
                .contains("\"code\":503")
                .contains("\"message\":\"Service temporarily unavailable\"");
    }

    private GatewayCircuitBreakerFilter createFilter() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties project = new GatewayProperties.ProjectProperties();
        project.setPathSegment("game");
        GatewayProperties.RouteProperties route = new GatewayProperties.RouteProperties();
        route.setPathSegment("admin");
        route.setServiceUri(URI.create("http://127.0.0.1:18080"));
        route.setServicePathPrefix("/admin");
        route.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        route.getGovernance().getCircuitBreaker().setEnabled(true);
        route.getGovernance().getCircuitBreaker().setSlidingWindowSize(1);
        route.getGovernance().getCircuitBreaker().setMinimumNumberOfCalls(1);
        route.getGovernance().getCircuitBreaker().setFailureRateThreshold(1D);
        project.getRoutes().put("admin", route);
        properties.getProjects().put("game", project);
        return new GatewayCircuitBreakerFilter(new GatewayRouteDefinitionLocator(properties), new DispatcherHandler());
    }

    private MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/game/admin/system/ping"));
    }
}
