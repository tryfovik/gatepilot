package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流量染色解析测试。
 */
class TrafficColorResolverTest {

    @Test
    void shouldTrustConfiguredRequestHeader() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of("X-Traffic-Color", "blue"),
                Map.of(),
                Map.of()
        ));

        assertThat(color).isEqualTo("blue");
    }

    @Test
    void shouldResolveColorFromPolicyRules() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of(),
                Map.of("beta", "true"),
                Map.of()
        ));

        assertThat(color).isEqualTo("green");
    }

    @Test
    void shouldResolveWeightedReleaseColorWhenNoRuleMatches() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of("X-User-Id", "u-10001"),
                Map.of(),
                Map.of()
        ));

        assertThat(color).isEqualTo("canary");
    }

    @Test
    void shouldReturnDefaultColorWhenNothingMatches() {
        CompiledProxyRuntime runtime = runtimeWithoutReleasePolicy();

        String color = new TrafficColorResolver().resolve(runtime, request(Map.of(), Map.of(), Map.of()));

        assertThat(color).isEqualTo("stable");
    }

    private CompiledProxyRuntime runtime() {
        PublishedConfig config = baseConfig();
        config.getSpec().getPolicies().add(trafficPolicy());
        config.getSpec().getPolicies().add(releasePolicy());
        return new PublishedConfigCompiler().compile(config);
    }

    private CompiledProxyRuntime runtimeWithoutReleasePolicy() {
        PublishedConfig config = baseConfig();
        config.getSpec().getPolicies().add(trafficPolicy());
        return new PublishedConfigCompiler().compile(config);
    }

    private PublishedConfig baseConfig() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("admin");
        route.getHosts().add("api.example.com");
        route.setPath("/api/game/admin");
        route.getPolicyNames().add("traffic-main");
        route.getPolicyNames().add("release-main");
        config.getSpec().getRoutes().add(route);
        return config;
    }

    private PublishedConfig.PublishedPolicy trafficPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic-main");
        policy.setType("TrafficPolicy");
        policy.getConfig().put("trustRequestHeader", true);
        policy.getConfig().put("defaultColor", "stable");
        policy.getConfig().put("colorRules", List.of(cookieRule()));
        return policy;
    }

    private TrafficPolicy.TrafficColorRule cookieRule() {
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.COOKIE);
        rule.setKey("beta");
        rule.setMatch("true");
        rule.setColor("green");
        return rule;
    }

    private PublishedConfig.PublishedPolicy releasePolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("release-main");
        policy.setType("ReleasePolicy");
        Map<String, Object> split = new LinkedHashMap<>();
        split.put("target", "canary");
        split.put("color", "canary");
        split.put("weight", 100);
        policy.getConfig().put("trafficSplits", List.of(split));
        return policy;
    }

    private TrafficColorRequest request(Map<String, String> headers,
                                        Map<String, String> cookies,
                                        Map<String, String> query) {
        return new TrafficColorRequest(
                "api.example.com",
                "/api/game/admin/users",
                null,
                headers::get,
                cookies::get,
                query::get,
                "127.0.0.1"
        );
    }
}
