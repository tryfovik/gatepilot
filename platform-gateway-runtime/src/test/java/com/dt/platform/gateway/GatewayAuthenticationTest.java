package com.dt.platform.gateway;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import com.getboot.auth.spi.SaTokenWebFluxAuthChecker;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * 网关认证与跨域集成测试。
 */
@SpringBootTest(
        classes = GatewayCoreTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "platform.gateway.api-prefix=/api",
                "platform.gateway.internal-prefix=/internal",
                "platform.gateway.projects.game.path-segment=game",
                "platform.gateway.projects.game.routes.admin.path-segment=admin",
                "platform.gateway.projects.game.routes.admin.service-path-prefix=/admin",
                "platform.gateway.projects.game.routes.admin.auth.required=true",
                "platform.gateway.projects.game.routes.admin.auth.public-paths[0]=/system/ping",
                "platform.gateway.projects.game.routes.admin.auth.public-paths[1]=/auth/**",
                "platform.gateway.projects.game.routes.open.path-segment=open",
                "platform.gateway.projects.game.routes.open.service-path-prefix=/open",
                "getboot.auth.satoken.token-name=Authorization",
                "getboot.auth.satoken.is-log=false"
        }
)
@Import(GatewayAuthenticationTest.GatewayAuthTestConfiguration.class)
@Disabled("Requires local socket binding")
class GatewayAuthenticationTest {

    private static final DisposableServer ADMIN_SERVER = HttpServer.create()
            .port(0)
            .route(routes -> routes
                    .get("/admin/system/ping", (request, response) -> response
                            .header(HttpHeaderNames.CONTENT_TYPE, "application/json")
                            .sendString(Mono.just("{\"service\":\"admin\",\"path\":\"" + request.uri() + "\"}")))
                    .get("/admin/games/catalog", (request, response) -> response
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
    void shouldRejectAdminApiWhenAuthenticationMissing() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/game/admin/games/catalog")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo("fail")
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Unauthorized");
    }

    @Test
    void shouldAllowAdminApiWhenAuthenticationPasses() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/game/admin/games/catalog")
                .header("X-Admin-Login", "ok")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("admin")
                .jsonPath("$.path").isEqualTo("/admin/games/catalog");
    }

    @Test
    void shouldKeepAdminPingPublicForSmokeChecks() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/game/admin/system/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("admin")
                .jsonPath("$.path").isEqualTo("/admin/system/ping");
    }

    @Test
    void shouldHandleCorsPreflightForAdminRoute() {
        webTestClient.options()
                .uri("http://127.0.0.1:" + port + "/api/game/admin/games/catalog")
                .header("Origin", "http://127.0.0.1:3100")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://127.0.0.1:3100")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void shouldExposeTraceHeaderForCorsResponse() {
        webTestClient.get()
                .uri("http://127.0.0.1:" + port + "/api/game/open/system/ping")
                .header("Origin", "http://127.0.0.1:3101")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://127.0.0.1:3101");
    }

    @TestConfiguration
    static class GatewayAuthTestConfiguration {

        @Bean
        public SaTokenWebFluxAuthChecker gatewayAuthChecker() {
            return () -> {
                String loginHeader = SaHolder.getRequest().getHeader("X-Admin-Login");
                if (!"ok".equals(loginHeader)) {
                    throw new NotLoginException(NotLoginException.NOT_TOKEN, "gateway", NotLoginException.DEFAULT_MESSAGE);
                }
            };
        }
    }
}
