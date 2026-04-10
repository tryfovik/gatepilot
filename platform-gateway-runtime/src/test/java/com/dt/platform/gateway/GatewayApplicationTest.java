package com.dt.platform.gateway;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * 网关核心集成测试。
 */
@SpringBootTest(
        classes = GatewayCoreTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "platform.gateway.api-prefix=/api",
                "platform.gateway.internal-prefix=/internal",
                "platform.gateway.projects.game.path-segment=game",
                "platform.gateway.projects.game.routes.admin.path-segment=admin",
                "platform.gateway.projects.game.routes.admin.legacy-path-segments[0]=admin",
                "platform.gateway.projects.game.routes.admin.service-path-prefix=/admin",
                "platform.gateway.projects.game.routes.open.path-segment=open",
                "platform.gateway.projects.game.routes.open.legacy-path-segments[0]=open",
                "platform.gateway.projects.game.routes.open.service-path-prefix=/open",
                "management.endpoints.web.exposure.include=health,info"
        }
)
@Disabled("Requires local socket binding")
class GatewayApplicationTest {

    private static final DisposableServer ADMIN_SERVER = HttpServer.create()
            .port(0)
            .route(routes -> routes
                    .get("/admin/system/ping", (request, response) -> response
                            .header(HttpHeaderNames.CONTENT_TYPE, "application/json")
                            .sendString(Mono.just("{\"service\":\"admin\",\"path\":\"" + request.uri() + "\"}")))
                    .get("/actuator/health", (request, response) -> response
                            .header(HttpHeaderNames.CONTENT_TYPE, "application/json")
                            .sendString(Mono.just("{\"status\":\"UP\",\"service\":\"admin\"}"))))
            .bindNow();

    private static final DisposableServer OPEN_SERVER = HttpServer.create()
            .port(0)
            .route(routes -> routes
                    .get("/open/system/ping", (request, response) -> response
                            .header(HttpHeaderNames.CONTENT_TYPE, "application/json")
                            .sendString(Mono.just("{\"service\":\"open\",\"path\":\"" + request.uri() + "\"}")))
                    .get("/actuator/health", (request, response) -> response
                            .header(HttpHeaderNames.CONTENT_TYPE, "application/json")
                            .sendString(Mono.just("{\"status\":\"UP\",\"service\":\"open\"}"))))
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void registerGatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.gateway.projects.game.routes.admin.service-uri", () -> "http://127.0.0.1:" + ADMIN_SERVER.port());
        registry.add("platform.gateway.projects.game.routes.admin.actuator-uri", () -> "http://127.0.0.1:" + ADMIN_SERVER.port());
        registry.add("platform.gateway.projects.game.routes.open.service-uri", () -> "http://127.0.0.1:" + OPEN_SERVER.port());
        registry.add("platform.gateway.projects.game.routes.open.actuator-uri", () -> "http://127.0.0.1:" + OPEN_SERVER.port());
    }

    @AfterAll
    static void shutdownServers() {
        ADMIN_SERVER.disposeNow();
        OPEN_SERVER.disposeNow();
    }

    @Test
    void shouldRouteAdminApiToAdminUpstream() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/admin/system/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("admin")
                .jsonPath("$.path").isEqualTo("/admin/system/ping");
    }

    @Test
    void shouldRouteOpenApiToOpenUpstream() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/open/system/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("open")
                .jsonPath("$.path").isEqualTo("/open/system/ping");
    }

    @Test
    void shouldRouteInternalAdminActuatorRequest() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/internal/admin/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.service").isEqualTo("admin");
    }

    @Test
    void shouldExposeGatewayUpstreamHealth() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP")
                .jsonPath("$.components.gatewayUpstreams.status").isEqualTo("UP")
                .jsonPath("$.components.gatewayUpstreams.details['game-admin'].status").isEqualTo("UP")
                .jsonPath("$.components.gatewayUpstreams.details['game-open'].status").isEqualTo("UP");
    }
}
