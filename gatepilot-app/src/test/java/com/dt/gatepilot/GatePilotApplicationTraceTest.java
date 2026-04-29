package com.dt.gatepilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * GatePilot 单体应用 Trace 集成测试。
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "gatepilot.agent.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
                        + "cn.dev33.satoken.dao.SaTokenDaoRedisJackson"
        }
)
@AutoConfigureWebTestClient
class GatePilotApplicationTraceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnTraceHeaderThroughGetbootObservability() {
        webTestClient.get()
                .uri("/api/gatepilot/v1/resources/projects?namespace=default")
                .header("X-Trace-Id", "trace-gatepilot-test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Trace-Id", "trace-gatepilot-test")
                .expectBody()
                .jsonPath("$.status").isEqualTo("success")
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.meta.traceId").isEqualTo("trace-gatepilot-test");
    }
}
