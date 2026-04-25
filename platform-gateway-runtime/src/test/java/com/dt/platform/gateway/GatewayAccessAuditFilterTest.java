package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.audit.GatewayAccessAuditEvent;
import com.dt.platform.gateway.infrastructure.audit.GatewayAccessAuditFilter;
import com.dt.platform.gateway.infrastructure.audit.GatewayAccessAuditRepository;
import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.traffic.GatewayTrafficColorFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关访问审计过滤器测试。
 */
class GatewayAccessAuditFilterTest {

    @Test
    void shouldRecordApiRouteAccessAuditEvent() {
        GatewayProperties properties = createGatewayProperties();
        properties.getAudit().setLogEnabled(false);
        GatewayAccessAuditRepository repository = new GatewayAccessAuditRepository(properties.getAudit());
        GatewayAccessAuditFilter filter = new GatewayAccessAuditFilter(
                properties,
                new GatewayRouteDefinitionLocator(properties),
                repository
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/admin/system/ping")
                        .header("X-Trace-Id", "trace-1")
        );
        exchange.getAttributes().put(GatewayTrafficColorFilter.TRAFFIC_COLOR_ATTRIBUTE, "green");

        filter.filter(exchange, chainExchange -> {
            chainExchange.getResponse().setStatusCode(HttpStatus.OK);
            return chainExchange.getResponse().setComplete();
        }).block();

        List<GatewayAccessAuditEvent> events =
                repository.search(new GatewayAccessAuditRepository.AuditQuery(null, null, null, null, null, null, null, 10));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).traceId()).isEqualTo("trace-1");
        assertThat(events.get(0).routeType()).isEqualTo("api");
        assertThat(events.get(0).projectKey()).isEqualTo("game");
        assertThat(events.get(0).routeKey()).isEqualTo("admin");
        assertThat(events.get(0).trafficColor()).isEqualTo("green");
        assertThat(events.get(0).releaseVariant()).isEqualTo("green");
        assertThat(events.get(0).status()).isEqualTo(200);
        assertThat(events.get(0).outcome()).isEqualTo("success");
    }

    @Test
    void shouldFallbackToRequestIdWhenTraceHeaderIsMissing() {
        GatewayProperties properties = createGatewayProperties();
        properties.getAudit().setLogEnabled(false);
        GatewayAccessAuditRepository repository = new GatewayAccessAuditRepository(properties.getAudit());
        GatewayAccessAuditFilter filter = new GatewayAccessAuditFilter(
                properties,
                new GatewayRouteDefinitionLocator(properties),
                repository
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/game/admin/system/ping")
        );

        filter.filter(exchange, chainExchange -> {
            chainExchange.getResponse().setStatusCode(HttpStatus.OK);
            return chainExchange.getResponse().setComplete();
        }).block();

        List<GatewayAccessAuditEvent> events =
                repository.search(new GatewayAccessAuditRepository.AuditQuery(null, null, null, null, null, null, null, 10));
        assertThat(events).hasSize(1);
        assertThat(events.get(0).traceId()).isEqualTo(exchange.getRequest().getId());
    }

    @Test
    void shouldKeepRecentAuditEventsWithinCapacity() {
        GatewayProperties properties = createGatewayProperties();
        properties.getAudit().setRecentCapacity(2);
        GatewayAccessAuditRepository repository = new GatewayAccessAuditRepository(properties.getAudit());

        repository.save(event("trace-1"));
        repository.save(event("trace-2"));
        repository.save(event("trace-3"));

        List<GatewayAccessAuditEvent> events =
                repository.search(new GatewayAccessAuditRepository.AuditQuery(null, null, null, null, null, null, null, 10));
        assertThat(events)
                .extracting(GatewayAccessAuditEvent::traceId)
                .containsExactly("trace-3", "trace-2");
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties project = new GatewayProperties.ProjectProperties();
        project.setPathSegment("game");

        GatewayProperties.RouteProperties route = new GatewayProperties.RouteProperties();
        route.setPathSegment("admin");
        route.setServiceUri(URI.create("http://127.0.0.1:18080"));
        route.setServicePathPrefix("/admin");
        route.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        route.getAuth().setRequired(true);
        route.getAuth().setPublicPaths(List.of("/system/ping"));

        GatewayProperties.ReleaseVariantProperties green = new GatewayProperties.ReleaseVariantProperties();
        green.setServiceUri(URI.create("http://127.0.0.1:28080"));
        route.getRelease().getVariants().put("green", green);

        project.getRoutes().put("admin", route);
        properties.getProjects().put("game", project);
        return properties;
    }

    private GatewayAccessAuditEvent event(String traceId) {
        return new GatewayAccessAuditEvent(
                Instant.now(),
                traceId,
                "127.0.0.1",
                "GET",
                "/api/game/admin/system/ping",
                "api",
                "game",
                "admin",
                200,
                1,
                "stable",
                null,
                URI.create("http://127.0.0.1:18080"),
                true,
                false,
                true,
                false,
                "success",
                null
        );
    }
}
