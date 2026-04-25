package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PublishedConfig 编译器测试。
 */
class PublishedConfigCompilerTest {

    @Test
    void shouldMatchRouteByHostAndLongestPathPrefix() {
        PublishedConfig config = config("v1", "hash-v1");
        config.getSpec().getRoutes().add(route("project", "API.EXAMPLE.COM:443", "/api/game"));
        config.getSpec().getRoutes().add(route("admin", "api.example.com", "/api/game/admin"));
        config.getSpec().getRoutes().add(route("public", null, "/api/public"));

        CompiledProxyRuntime runtime = new PublishedConfigCompiler().compile(config);

        assertThat(runtime.match("api.example.com", "/api/game/admin/users").getRouteId()).isEqualTo("admin");
        assertThat(runtime.match("api.example.com:443", "/api/game/orders").getRouteId()).isEqualTo("project");
        assertThat(runtime.match("other.example.com", "/api/public/ping").getRouteId()).isEqualTo("public");
        assertThat(runtime.match("other.example.com", "/api/missing")).isNull();
    }

    @Test
    void shouldRejectDuplicateRouteMatchKey() {
        PublishedConfig config = config("v1", "hash-v1");
        config.getSpec().getRoutes().add(route("one", "api.example.com", "/api/game"));
        config.getSpec().getRoutes().add(route("two", "API.EXAMPLE.COM:443", "/api/game/"));

        assertThatThrownBy(() -> new PublishedConfigCompiler().compile(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate route match key");
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }

    private PublishedConfig.PublishedRoute route(String routeId, String host, String path) {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId(routeId);
        if (host != null) {
            route.getHosts().add(host);
        }
        route.setPath(path);
        return route;
    }
}
