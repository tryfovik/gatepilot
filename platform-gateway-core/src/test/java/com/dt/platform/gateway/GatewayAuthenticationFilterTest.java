package com.dt.platform.gateway;

import cn.dev33.satoken.exception.NotLoginException;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.security.GatewayAuthenticationFilter;
import com.getboot.auth.spi.SaTokenWebFluxAuthChecker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关认证过滤器测试。
 */
class GatewayAuthenticationFilterTest {

    @Test
    void shouldSkipAuthenticationForPublicRoutePath() {
        GatewayAuthenticationFilter filter = createFilter(() -> {
            throw new NotLoginException(NotLoginException.NOT_TOKEN, "gateway", "Unauthorized");
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/system/ping"));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldRejectProtectedRouteWhenAuthenticationFails() {
        GatewayAuthenticationFilter filter = createFilter(() -> {
            throw new NotLoginException(NotLoginException.NOT_TOKEN, "gateway", "Unauthorized");
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/admin/games/catalog"));

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldSkipOptionsRequestWhenConfigured() {
        GatewayAuthenticationFilter filter = createFilter(() -> {
            throw new AssertionError("auth checker should not be invoked");
        });
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/admin/games/catalog"));
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
    }

    private GatewayAuthenticationFilter createFilter(SaTokenWebFluxAuthChecker authChecker) {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties gameProject = new GatewayProperties.ProjectProperties();
        gameProject.setPathSegment("game");

        GatewayProperties.RouteProperties adminRoute = new GatewayProperties.RouteProperties();
        adminRoute.setPathSegment("admin");
        adminRoute.setLegacyPathSegments(java.util.List.of("admin"));
        adminRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setServicePathPrefix("/admin");
        adminRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.getAuth().setRequired(true);
        adminRoute.getAuth().setPublicPaths(java.util.List.of("/system/ping", "/auth/**"));
        gameProject.getRoutes().put("admin", adminRoute);
        properties.getProjects().put("game", gameProject);

        return new GatewayAuthenticationFilter(
                properties.getAuth(),
                new GatewayRouteDefinitionLocator(properties),
                authChecker
        );
    }

    private WebFilterChain markInvoked(AtomicBoolean chainInvoked) {
        return exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
    }
}
