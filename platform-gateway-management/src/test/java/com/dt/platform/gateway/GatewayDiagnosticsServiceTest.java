package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.diagnostics.GatewayDiagnosticsService;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关路由诊断服务测试。
 */
class GatewayDiagnosticsServiceTest {

    @Test
    void shouldDiagnoseApiRouteWithTrafficColorAndReleaseVariant() {
        GatewayDiagnosticsService diagnosticsService = createDiagnosticsService();
        GatewayDiagnosticsService.GatewayDiagnosticsRequest request =
                new GatewayDiagnosticsService.GatewayDiagnosticsRequest(
                        "GET",
                        "/api/game/admin/system/ping",
                        Map.of("X-Canary", List.of("true"), "X-User-Id", List.of("alice")),
                        Map.of(),
                        Map.of(),
                        "127.0.0.1"
                );

        GatewayDiagnosticsService.GatewayDiagnosticsView view = diagnosticsService.diagnose(request);

        assertThat(view.matched()).isTrue();
        assertThat(view.routeType()).isEqualTo("api");
        assertThat(view.route().projectKey()).isEqualTo("game");
        assertThat(view.route().routeKey()).isEqualTo("admin");
        assertThat(view.access().methodAllowed()).isTrue();
        assertThat(view.access().publicPath()).isTrue();
        assertThat(view.access().authenticationRequired()).isFalse();
        assertThat(view.traffic().color()).isEqualTo("green");
        assertThat(view.traffic().selectedReleaseVariant().variantKey()).isEqualTo("green");
        assertThat(view.upstream().releaseVariant()).isEqualTo("green");
        assertThat(view.upstream().uri()).isEqualTo(URI.create("http://127.0.0.1:28080"));
        assertThat(view.governance().flowControl().enabled()).isTrue();
        assertThat(view.governance().retry().enabled()).isTrue();
        assertThat(view.governance().circuitBreaker().enabled()).isTrue();
    }

    @Test
    void shouldReportDisallowedMethodAndAuthRequirement() {
        GatewayDiagnosticsService diagnosticsService = createDiagnosticsService();
        GatewayDiagnosticsService.GatewayDiagnosticsRequest request =
                new GatewayDiagnosticsService.GatewayDiagnosticsRequest(
                        "DELETE",
                        "/api/game/admin/users",
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        "127.0.0.1"
                );

        GatewayDiagnosticsService.GatewayDiagnosticsView view = diagnosticsService.diagnose(request);

        assertThat(view.matched()).isTrue();
        assertThat(view.access().methodAllowed()).isFalse();
        assertThat(view.access().authenticationRequired()).isTrue();
        assertThat(view.warnings()).contains(
                "HTTP method is not allowed for the matched API route",
                "Request requires authentication for the matched API route"
        );
    }

    @Test
    void shouldReportUnmatchedRoute() {
        GatewayDiagnosticsService diagnosticsService = createDiagnosticsService();
        GatewayDiagnosticsService.GatewayDiagnosticsRequest request =
                new GatewayDiagnosticsService.GatewayDiagnosticsRequest(
                        "GET",
                        "/api/missing/admin/ping",
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        "127.0.0.1"
                );

        GatewayDiagnosticsService.GatewayDiagnosticsView view = diagnosticsService.diagnose(request);

        assertThat(view.matched()).isFalse();
        assertThat(view.routeType()).isEqualTo("none");
        assertThat(view.warnings()).contains("No gateway route matched the request path");
    }

    private GatewayDiagnosticsService createDiagnosticsService() {
        GatewayProperties properties = createGatewayProperties();
        return new GatewayDiagnosticsService(properties, new GatewayRouteDefinitionLocator(properties));
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        properties.getTrafficColor().setEnabled(true);
        GatewayProperties.TrafficColorRuleProperties trafficRule = new GatewayProperties.TrafficColorRuleProperties();
        trafficRule.setSource("header");
        trafficRule.setFieldName("X-Canary");
        trafficRule.setPattern("true");
        trafficRule.setColor("green");
        properties.getTrafficColor().setRules(List.of(trafficRule));

        GatewayProperties.ProjectProperties project = new GatewayProperties.ProjectProperties();
        project.setPathSegment("game");

        GatewayProperties.RouteProperties route = new GatewayProperties.RouteProperties();
        route.setPathSegment("admin");
        route.setServiceUri(URI.create("http://127.0.0.1:18080"));
        route.setServicePathPrefix("/admin");
        route.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        route.setApiMethods(List.of("GET", "POST"));
        route.getAuth().setRequired(true);
        route.getAuth().setPublicPaths(List.of("/system/ping"));
        route.getGovernance().getFlowControl().setEnabled(true);
        route.getGovernance().getFlowControl().setCount(100D);
        route.getGovernance().getRetry().setEnabled(true);
        route.getGovernance().getRetry().setRetries(2);
        route.getGovernance().getCircuitBreaker().setEnabled(true);
        route.getGovernance().getCircuitBreaker().setMinimumNumberOfCalls(1);
        route.getGovernance().getCircuitBreaker().setSlidingWindowSize(10);
        route.getGovernance().getCircuitBreaker().setFailureRateThreshold(50D);
        route.getGovernance().getCircuitBreaker().setWaitDurationInOpenState(Duration.ofSeconds(30));

        GatewayProperties.ReleaseVariantProperties greenVariant = new GatewayProperties.ReleaseVariantProperties();
        greenVariant.setServiceUri(URI.create("http://127.0.0.1:28080"));
        greenVariant.setActuatorUri(URI.create("http://127.0.0.1:28080"));
        greenVariant.setWeight(10);
        route.getRelease().getVariants().put("green", greenVariant);

        project.getRoutes().put("admin", route);
        properties.getProjects().put("game", project);
        return properties;
    }
}
