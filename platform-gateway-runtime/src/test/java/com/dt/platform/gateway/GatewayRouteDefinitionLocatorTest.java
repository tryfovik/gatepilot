package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由定义定位器测试。
 */
class GatewayRouteDefinitionLocatorTest {

    @Test
    void shouldBuildCanonicalAndLegacyPathsForProjectRoute() {
        GatewayRouteDefinitionLocator locator = new GatewayRouteDefinitionLocator(createGatewayProperties());

        GatewayRouteDefinition definition = locator.findApiRoute("/api/game/admin/system/ping").orElseThrow();
        assertThat(definition.getProjectKey()).isEqualTo("game");
        assertThat(definition.getRouteKey()).isEqualTo("admin");
        assertThat(definition.getApiPathRoots()).containsExactly("/api/game/admin", "/api/admin");
        assertThat(definition.matchesApiPath("/api/admin/system/ping")).isTrue();
        assertThat(definition.matchesInternalPath("/internal/game/admin/actuator/health")).isTrue();
        assertThat(definition.isPublicApiPath("/api/admin/system/ping")).isTrue();
        assertThat(definition.isPublicApiPath("/api/admin/games/catalog")).isFalse();
        assertThat(definition.getConnectTimeoutMs()).isEqualTo(2000);
        assertThat(definition.getResponseTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void shouldRejectDuplicateLegacyPathSegmentsAcrossRoutes() {
        GatewayProperties properties = createGatewayProperties();
        properties.getProjects().get("game").getRoutes().get("open").setLegacyPathSegments(java.util.List.of("admin"));

        assertThatThrownBy(() -> new GatewayRouteDefinitionLocator(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gateway route path must be unique among enabled routes");
    }

    @Test
    void shouldRejectMissingServiceUriWhenApiRouteEnabled() {
        GatewayProperties properties = createGatewayProperties();
        properties.getProjects().get("game").getRoutes().get("admin").setServiceUri(null);

        assertThatThrownBy(() -> new GatewayRouteDefinitionLocator(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("gateway route service uri must not be null");
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties gameProject = new GatewayProperties.ProjectProperties();
        gameProject.setPathSegment("game");

        GatewayProperties.RouteProperties adminRoute = new GatewayProperties.RouteProperties();
        adminRoute.setPathSegment("admin");
        adminRoute.setLegacyPathSegments(java.util.List.of("admin"));
        adminRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setServicePathPrefix("/admin");
        adminRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        adminRoute.setConnectTimeoutMs(2000);
        adminRoute.setResponseTimeout(Duration.ofSeconds(5));
        adminRoute.getAuth().setRequired(true);
        adminRoute.getAuth().setPublicPaths(java.util.List.of("/system/ping", "/auth/**"));

        GatewayProperties.RouteProperties openRoute = new GatewayProperties.RouteProperties();
        openRoute.setPathSegment("open");
        openRoute.setLegacyPathSegments(java.util.List.of("open"));
        openRoute.setServiceUri(URI.create("http://127.0.0.1:18080"));
        openRoute.setServicePathPrefix("/open");
        openRoute.setActuatorUri(URI.create("http://127.0.0.1:18080"));

        gameProject.getRoutes().put("admin", adminRoute);
        gameProject.getRoutes().put("open", openRoute);
        properties.getProjects().put("game", gameProject);
        return properties;
    }
}
