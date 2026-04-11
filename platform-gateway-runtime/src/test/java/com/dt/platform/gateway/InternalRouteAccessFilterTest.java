package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.security.InternalRouteAccessFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内部路由访问过滤器测试。
 */
class InternalRouteAccessFilterTest {

    @Test
    void shouldRejectInternalRouteWhenRemoteAddressIsNotLoopback() {
        GatewayProperties properties = new GatewayProperties();
        InternalRouteAccessFilter filter = new InternalRouteAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/game/admin/actuator/health")
                        .remoteAddress(new InetSocketAddress("192.168.10.8", 5000))
        );

        filter.filter(exchange, exchange1 -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAllowInternalRouteWhenRemoteAddressIsLoopback() {
        GatewayProperties properties = new GatewayProperties();
        InternalRouteAccessFilter filter = new InternalRouteAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/internal/game/admin/actuator/health")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 5000))
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldIgnoreApiRoute() {
        GatewayProperties properties = new GatewayProperties();
        InternalRouteAccessFilter filter = new InternalRouteAccessFilter(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/open/system/ping")
                        .remoteAddress(new InetSocketAddress("192.168.10.8", 5000))
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.filter(exchange, markInvoked(chainInvoked)).block();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private WebFilterChain markInvoked(AtomicBoolean chainInvoked) {
        return exchange -> {
            chainInvoked.set(true);
            return Mono.empty();
        };
    }
}
