package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 路由访问策略判断测试。
 */
class RouteAccessEvaluatorTest {

    @Test
    void shouldEvaluateMethodAndAuthenticationPolicy() {
        RouteAccessEvaluator evaluator = new RouteAccessEvaluator();
        CompiledProxyRuntime runtime = runtime();

        RouteAccessDecision decision = evaluator.evaluate(runtime,
                new RouteAccessRequest("api.example.com", "/api/game/admin/orders", "GET"));

        assertThat(decision.matched()).isTrue();
        assertThat(decision.methodAllowed()).isTrue();
        assertThat(decision.authenticationRequired()).isTrue();
        assertThat(decision.allowedMethods()).containsExactly("GET", "POST");
    }

    @Test
    void shouldRejectUnsupportedMethodBeforeAuthentication() {
        RouteAccessDecision decision = new RouteAccessEvaluator().evaluate(runtime(),
                new RouteAccessRequest("api.example.com", "/api/game/admin/orders", "DELETE"));

        assertThat(decision.matched()).isTrue();
        assertThat(decision.methodAllowed()).isFalse();
        assertThat(decision.authenticationRequired()).isFalse();
        assertThat(decision.allowedMethods()).containsExactly("GET", "POST");
    }

    @Test
    void shouldSkipAuthenticationForPublicPath() {
        RouteAccessDecision decision = new RouteAccessEvaluator().evaluate(runtime(),
                new RouteAccessRequest("api.example.com", "/api/game/admin/public/ping", "GET"));

        assertThat(decision.matched()).isTrue();
        assertThat(decision.methodAllowed()).isTrue();
        assertThat(decision.authenticationRequired()).isFalse();
    }

    private CompiledProxyRuntime runtime() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("admin");
        route.getHosts().add("api.example.com");
        route.setPath("/api/game/admin");
        route.setMethods(List.of(HttpMethod.GET, HttpMethod.POST));
        route.getPolicyNames().add("auth-main");
        config.getSpec().getRoutes().add(route);

        PublishedConfig.PublishedPolicy auth = new PublishedConfig.PublishedPolicy();
        auth.setName("auth-main");
        auth.setType("AuthPolicy");
        auth.getConfig().put("type", "JWT");
        auth.getConfig().put("anonymousAllowed", false);
        auth.getConfig().put("publicPaths", List.of("/public"));
        config.getSpec().getPolicies().add(auth);
        return new PublishedConfigCompiler().compile(config);
    }
}
