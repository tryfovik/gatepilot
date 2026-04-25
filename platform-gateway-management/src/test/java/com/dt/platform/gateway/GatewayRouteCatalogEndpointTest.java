package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.management.GatewayConfigStore;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteCatalogEndpoint;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 生效路由目录端点测试。
 */
class GatewayRouteCatalogEndpointTest {

    @Test
    void shouldDescribeEffectiveProjectRoutes() {
        GatewayProperties properties = createGatewayProperties();
        GatewayRouteCatalogEndpoint endpoint = new GatewayRouteCatalogEndpoint(new GatewayConfigStore(properties));

        GatewayRouteCatalogEndpoint.GatewayRouteCatalogView view = endpoint.routes();

        assertThat(view.apiPrefix()).isEqualTo("/api");
        assertThat(view.internalPrefix()).isEqualTo("/internal");
        assertThat(view.contextHeaders().enabled()).isTrue();
        assertThat(view.contextHeaders().projectHeaderName()).isEqualTo("X-Platform-Project");
        assertThat(view.trafficColor().enabled()).isTrue();
        assertThat(view.trafficColor().headerName()).isEqualTo("X-Traffic-Color");
        assertThat(view.trafficColor().defaultColor()).isEqualTo("stable");
        assertThat(view.trafficColor().rules()).hasSize(1);
        assertThat(view.trafficColor().rules().get(0).fieldName()).isEqualTo("X-Canary");
        assertThat(view.projects()).hasSize(1);

        GatewayRouteCatalogEndpoint.ProjectView project = view.projects().get(0);
        assertThat(project.projectKey()).isEqualTo("game");
        assertThat(project.pathSegment()).isEqualTo("game");
        assertThat(project.displayName()).isEqualTo("Game Platform");
        assertThat(project.routes()).hasSize(2);

        GatewayRouteCatalogEndpoint.RouteView adminRoute = project.routes().get(0);
        assertThat(adminRoute.routeKey()).isEqualTo("admin");
        assertThat(adminRoute.apiPathRoots()).containsExactly("/api/game/admin");
        assertThat(adminRoute.internalPathRoots()).containsExactly("/internal/game/admin");
        assertThat(adminRoute.serviceUri()).isEqualTo(URI.create("http://127.0.0.1:18080"));
        assertThat(adminRoute.apiMethods()).containsExactly("GET", "POST");
        assertThat(adminRoute.apiMaxRequestSizeBytes()).isEqualTo(10L * 1024 * 1024);
        assertThat(adminRoute.servicePathPrefix()).isEqualTo("/admin");
        assertThat(adminRoute.actuatorUri()).isEqualTo(URI.create("http://127.0.0.1:18080"));
        assertThat(adminRoute.internalMethods()).containsExactly("GET");
        assertThat(adminRoute.internalMaxRequestSizeBytes()).isEqualTo(1024L * 1024);
        assertThat(adminRoute.authRequired()).isTrue();
        assertThat(adminRoute.publicPaths()).containsExactly("/system/ping", "/auth/**");
        assertThat(adminRoute.connectTimeoutMs()).isEqualTo(2000);
        assertThat(adminRoute.responseTimeoutMs()).isEqualTo(5000L);
        assertThat(adminRoute.flowControl().enabled()).isTrue();
        assertThat(adminRoute.flowControl().resourceName()).isEqualTo("platform-gateway-api-game-admin");
        assertThat(adminRoute.flowControl().count()).isEqualTo(120D);
        assertThat(adminRoute.flowControl().controlBehavior()).isEqualTo("rate-limiter");
        assertThat(adminRoute.flowControl().param().enabled()).isTrue();
        assertThat(adminRoute.flowControl().param().fieldName()).isEqualTo("X-Tenant-Id");
        assertThat(adminRoute.retry().enabled()).isTrue();
        assertThat(adminRoute.retry().retries()).isEqualTo(2);
        assertThat(adminRoute.retry().methods()).containsExactly("GET");
        assertThat(adminRoute.retry().statuses()).containsExactly(502, 503, 504);
        assertThat(adminRoute.retry().series()).containsExactly("server-error");
        assertThat(adminRoute.retry().exceptions()).containsExactly("io", "timeout");
        assertThat(adminRoute.retry().backoff()).isNotNull();
        assertThat(adminRoute.retry().backoff().firstBackoffMs()).isEqualTo(20L);
        assertThat(adminRoute.retry().backoff().maxBackoffMs()).isEqualTo(200L);
        assertThat(adminRoute.circuitBreaker().enabled()).isTrue();
        assertThat(adminRoute.circuitBreaker().name()).isEqualTo("platform-gateway-cb-game-admin");
        assertThat(adminRoute.circuitBreaker().statusCodes()).containsExactly(500, 502, 503, 504);
        assertThat(adminRoute.circuitBreaker().fallbackUri()).isNull();
        assertThat(adminRoute.releaseWeightHashHeaders()).containsExactly("X-User-Id", "X-Tenant-Id", "X-Trace-Id");
        assertThat(adminRoute.releaseVariants()).hasSize(1);
        assertThat(adminRoute.releaseVariants().get(0).variantKey()).isEqualTo("green");
        assertThat(adminRoute.releaseVariants().get(0).matchColors()).containsExactly("green");
        assertThat(adminRoute.releaseVariants().get(0).weight()).isEqualTo(10);
        assertThat(adminRoute.releaseVariants().get(0).serviceUri()).isEqualTo(URI.create("http://127.0.0.1:28080"));
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        properties.getTrafficColor().setEnabled(true);
        GatewayProperties.TrafficColorRuleProperties trafficRule = new GatewayProperties.TrafficColorRuleProperties();
        trafficRule.setName("canary-header");
        trafficRule.setSource("header");
        trafficRule.setFieldName("X-Canary");
        trafficRule.setPattern("true");
        trafficRule.setColor("green");
        properties.getTrafficColor().setRules(java.util.List.of(trafficRule));
        GatewayProperties.ProjectProperties gameProject = new GatewayProperties.ProjectProperties();
        gameProject.setPathSegment("game");
        gameProject.setDisplayName("Game Platform");

        GatewayProperties.RouteProperties adminRoute = new GatewayProperties.RouteProperties();
        adminRoute.setPathSegment("admin");
        adminRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setApiMethods(java.util.List.of("get", "post"));
        adminRoute.setApiMaxRequestSize(org.springframework.util.unit.DataSize.ofMegabytes(10));
        adminRoute.setServicePathPrefix("/admin");
        adminRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setInternalMethods(java.util.List.of("GET"));
        adminRoute.setInternalMaxRequestSize(org.springframework.util.unit.DataSize.ofMegabytes(1));
        adminRoute.setConnectTimeoutMs(2000);
        adminRoute.setResponseTimeout(Duration.ofSeconds(5));
        adminRoute.getAuth().setRequired(true);
        adminRoute.getAuth().setPublicPaths(java.util.List.of("/system/ping", "/auth/**"));
        adminRoute.getGovernance().getFlowControl().setEnabled(true);
        adminRoute.getGovernance().getFlowControl().setCount(120D);
        adminRoute.getGovernance().getFlowControl().setBurst(20);
        adminRoute.getGovernance().getFlowControl().setControlBehavior("rate-limiter");
        adminRoute.getGovernance().getFlowControl().setMaxQueueingTimeoutMs(300);
        adminRoute.getGovernance().getFlowControl().getParam().setEnabled(true);
        adminRoute.getGovernance().getFlowControl().getParam().setParseStrategy("header");
        adminRoute.getGovernance().getFlowControl().getParam().setFieldName("X-Tenant-Id");
        adminRoute.getGovernance().getFlowControl().getParam().setPattern("vip-.*");
        adminRoute.getGovernance().getFlowControl().getParam().setMatchStrategy("regex");
        adminRoute.getGovernance().getRetry().setEnabled(true);
        adminRoute.getGovernance().getRetry().setRetries(2);
        adminRoute.getGovernance().getRetry().setMethods(java.util.List.of("GET"));
        adminRoute.getGovernance().getRetry().setStatuses(java.util.List.of(502, 503, 504));
        adminRoute.getGovernance().getRetry().setBackoff(new GatewayProperties.RetryBackoffProperties());
        adminRoute.getGovernance().getRetry().getBackoff().setFirstBackoff(Duration.ofMillis(20));
        adminRoute.getGovernance().getRetry().getBackoff().setMaxBackoff(Duration.ofMillis(200));
        adminRoute.getGovernance().getRetry().getBackoff().setFactor(2);
        adminRoute.getGovernance().getRetry().getBackoff().setBasedOnPreviousValue(true);
        adminRoute.getGovernance().getCircuitBreaker().setEnabled(true);
        GatewayProperties.ReleaseVariantProperties greenVariant = new GatewayProperties.ReleaseVariantProperties();
        greenVariant.setWeight(10);
        greenVariant.setServiceUri(URI.create("http://127.0.0.1:28080"));
        greenVariant.setActuatorUri(URI.create("http://127.0.0.1:28080"));
        adminRoute.getRelease().getVariants().put("green", greenVariant);

        GatewayProperties.RouteProperties openRoute = new GatewayProperties.RouteProperties();
        openRoute.setPathSegment("open");
        openRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        openRoute.setServicePathPrefix("/open");
        openRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));

        gameProject.getRoutes().put("admin", adminRoute);
        gameProject.getRoutes().put("open", openRoute);
        properties.getProjects().put("game", gameProject);
        return properties;
    }
}
