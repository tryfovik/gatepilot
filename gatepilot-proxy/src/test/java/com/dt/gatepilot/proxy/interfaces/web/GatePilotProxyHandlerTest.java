package com.dt.gatepilot.proxy.interfaces.web;

import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * proxy WebFlux 入口处理器测试。
 */
class GatePilotProxyHandlerTest {

    @Test
    void shouldForwardRequestWithRewrittenPathAndTrafficColorHeader() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        GatePilotProxyHandler handler = handler(runtime(), forwardedRequest);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users?preview=enabled")
                .header(HttpHeaders.HOST, "api.example.com")
                .header("X-Remove-Me", "bad")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Traffic-Color", "yellow")
                .expectBody(String.class).isEqualTo("ok");

        assertThat(forwardedRequest.get().url().toString())
                .isEqualTo("http://upstream.local:8080/admin/users?preview=enabled");
        assertThat(forwardedRequest.get().headers().getFirst("X-Traffic-Color")).isEqualTo("yellow");
        assertThat(forwardedRequest.get().headers().getFirst("X-Route-Id")).isEqualTo("admin");
        assertThat(forwardedRequest.get().headers()).doesNotContainKey("X-Remove-Me");
    }

    @Test
    void shouldRejectUnsupportedMethodBeforeForwarding() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        GatePilotProxyHandler handler = handler(runtime(), forwardedRequest);
        WebTestClient client = client(handler);

        client.post()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .expectHeader().valueEquals(HttpHeaders.ALLOW, "GET");

        assertThat(forwardedRequest.get()).isNull();
    }

    @Test
    void shouldReturnServiceUnavailableWhenRuntimeIsEmpty() {
        AtomicReference<ClientRequest> forwardedRequest = new AtomicReference<>();
        GatePilotProxyHandler handler = handler(new ProxyRuntimeState(), forwardedRequest);
        WebTestClient client = client(handler);

        client.get()
                .uri("/api/game/admin/users")
                .header(HttpHeaders.HOST, "api.example.com")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private WebTestClient client(GatePilotProxyHandler handler) {
        return WebTestClient.bindToRouterFunction(RouterFunctions.route(RequestPredicates.all(), handler::handle))
                .build();
    }

    private GatePilotProxyHandler handler(ProxyRuntimeState runtimeState,
                                          AtomicReference<ClientRequest> forwardedRequest) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    forwardedRequest.set(request);
                    return Mono.just(ClientResponse.create(HttpStatus.OK).body("ok").build());
                })
                .build();
        return new GatePilotProxyHandler(
                runtimeState,
                new RouteAccessEvaluator(),
                new TrafficColorResolver(),
                webClient
        );
    }

    private ProxyRuntimeState runtime() {
        ProxyRuntimeState state = new ProxyRuntimeState();
        state.switchTo(new PublishedConfigCompiler().compile(config()));
        return state;
    }

    private PublishedConfig config() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        config.getSpec().getRoutes().add(route());
        config.getSpec().getUpstreams().add(upstream());
        config.getSpec().getPolicies().add(trafficPolicy());
        return config;
    }

    private PublishedConfig.PublishedRoute route() {
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("admin");
        route.getHosts().add("api.example.com");
        route.setPath("/api/game/admin");
        route.getMethods().add(HttpMethod.GET);
        route.setUpstreamName("admin-upstream");
        route.setStripPrefix(true);
        route.setRewritePathPrefix("/admin");
        route.getAddHeaders().put("X-Route-Id", "admin");
        route.getRemoveHeaders().add("X-Remove-Me");
        route.getPolicyNames().add("traffic-main");
        return route;
    }

    private PublishedConfig.PublishedUpstream upstream() {
        PublishedConfig.PublishedUpstream upstream = new PublishedConfig.PublishedUpstream();
        upstream.setName("admin-upstream");
        upstream.setProtocol(Protocol.HTTP);
        PublishedConfig.PublishedEndpoint endpoint = new PublishedConfig.PublishedEndpoint();
        endpoint.setHost("upstream.local");
        endpoint.setPort(8080);
        endpoint.setWeight(100);
        upstream.getEndpoints().add(endpoint);
        return upstream;
    }

    private PublishedConfig.PublishedPolicy trafficPolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic-main");
        policy.setType("TrafficPolicy");
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.QUERY);
        rule.setKey("preview");
        rule.setMatch("enabled");
        rule.setColor("yellow");
        policy.getConfig().put("colorRules", List.of(rule));
        return policy;
    }
}
