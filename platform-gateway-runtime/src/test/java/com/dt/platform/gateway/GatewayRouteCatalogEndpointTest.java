package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteCatalogEndpoint;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
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
        GatewayRouteCatalogEndpoint endpoint = new GatewayRouteCatalogEndpoint(
                properties,
                new GatewayRouteDefinitionLocator(properties)
        );

        GatewayRouteCatalogEndpoint.GatewayRouteCatalogView view = endpoint.routes();

        assertThat(view.apiPrefix()).isEqualTo("/api");
        assertThat(view.internalPrefix()).isEqualTo("/internal");
        assertThat(view.contextHeaders().enabled()).isTrue();
        assertThat(view.contextHeaders().projectHeaderName()).isEqualTo("X-Platform-Project");
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
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
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
