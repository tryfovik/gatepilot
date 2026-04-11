package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.security.GatewayMethodAccessFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关方法访问过滤器测试。
 */
class GatewayMethodAccessFilterTest {

    @Test
    void shouldAllowConfiguredApiMethod() {
        GatewayMethodAccessFilter filter = createFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/game/admin/games/catalog")
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldRejectDisallowedApiMethodWithAllowHeader() {
        GatewayMethodAccessFilter filter = createFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.DELETE, "/api/game/admin/games/catalog")
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET, POST");
    }

    @Test
    void shouldRejectDisallowedInternalMethodWithAllowHeader() {
        GatewayMethodAccessFilter filter = createFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/internal/game/admin/actuator/health")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 5000))
        );

        filter.filter(exchange, ignored -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ALLOW)).isEqualTo("GET");
    }

    @Test
    void shouldIgnoreRouteWithoutMethodRestrictions() {
        GatewayMethodAccessFilter filter = createFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.DELETE, "/api/game/open/games/catalog")
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private GatewayMethodAccessFilter createFilter() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties gameProject = new GatewayProperties.ProjectProperties();
        gameProject.setPathSegment("game");

        GatewayProperties.RouteProperties adminRoute = new GatewayProperties.RouteProperties();
        adminRoute.setPathSegment("admin");
        adminRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setApiMethods(java.util.List.of("GET", "POST"));
        adminRoute.setServicePathPrefix("/admin");
        adminRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setInternalMethods(java.util.List.of("GET"));
        gameProject.getRoutes().put("admin", adminRoute);

        GatewayProperties.RouteProperties openRoute = new GatewayProperties.RouteProperties();
        openRoute.setPathSegment("open");
        openRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        openRoute.setServicePathPrefix("/open");
        openRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        gameProject.getRoutes().put("open", openRoute);

        properties.getProjects().put("game", gameProject);
        return new GatewayMethodAccessFilter(new GatewayRouteDefinitionLocator(properties));
    }

    private WebFilterChain markInvoked(AtomicBoolean chainInvoked) {
        return exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
    }
}
