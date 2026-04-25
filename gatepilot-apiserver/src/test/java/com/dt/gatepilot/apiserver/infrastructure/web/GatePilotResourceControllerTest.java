package com.dt.gatepilot.apiserver.infrastructure.web;

import com.dt.gatepilot.apiserver.api.response.CursorPageResponse;
import com.dt.gatepilot.apiserver.support.service.GatePilotResourceService;
import com.getboot.web.api.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot 资源 API 测试。
 */
@WebFluxTest(GatePilotResourceController.class)
@ContextConfiguration(classes = {
        GatePilotResourceController.class,
        GatePilotResourceControllerTest.TestApplication.class
})
class GatePilotResourceControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private StubGatePilotResourceService resourceService;

    @Test
    void shouldWrapResourceListWithGetbootApiResponse() {
        CursorPageResponse<Object> page = new CursorPageResponse<>();
        page.setLimit(50);
        page.setTotal(0);
        resourceService.setPage(page);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/gatepilot/v1/resources/projects")
                        .queryParam("namespace", "default")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(ApiResponse.SUCCESS_STATUS)
                .jsonPath("$.code").isEqualTo(ApiResponse.SUCCESS_CODE)
                .jsonPath("$.message").isEqualTo(ApiResponse.DEFAULT_SUCCESS_MESSAGE)
                .jsonPath("$.data.limit").isEqualTo(50)
                .jsonPath("$.data.total").isEqualTo(0);

        assertThat(resourceService.getLastResourcePath()).isEqualTo("projects");
        assertThat(resourceService.getLastNamespace()).isEqualTo("default");
        assertThat(resourceService.getLastCursor()).isNull();
        assertThat(resourceService.getLastLimit()).isNull();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        StubGatePilotResourceService gatePilotResourceService() {
            return new StubGatePilotResourceService();
        }
    }

    static class StubGatePilotResourceService extends GatePilotResourceService {

        private CursorPageResponse<Object> page = new CursorPageResponse<>();

        private String lastResourcePath;

        private String lastNamespace;

        private String lastCursor;

        private Integer lastLimit;

        StubGatePilotResourceService() {
            super(null, null, null);
        }

        @Override
        public CursorPageResponse<Object> list(String resourcePath, String namespace, String cursor, Integer limit) {
            this.lastResourcePath = resourcePath;
            this.lastNamespace = namespace;
            this.lastCursor = cursor;
            this.lastLimit = limit;
            return page;
        }

        void setPage(CursorPageResponse<Object> page) {
            this.page = page;
        }

        String getLastResourcePath() {
            return lastResourcePath;
        }

        String getLastNamespace() {
            return lastNamespace;
        }

        String getLastCursor() {
            return lastCursor;
        }

        Integer getLastLimit() {
            return lastLimit;
        }
    }
}
