/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.application.service.GatePilotResourceService;
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
        CursorPage<Object> page = new CursorPage<>();
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

        private CursorPage<Object> page = new CursorPage<>();

        private String lastResourcePath;

        private String lastNamespace;

        private String lastCursor;

        private Integer lastLimit;

        StubGatePilotResourceService() {
            super(null, null, null);
        }

        @Override
        public CursorPage<Object> list(String resourcePath, String namespace, String cursor, Integer limit) {
            this.lastResourcePath = resourcePath;
            this.lastNamespace = namespace;
            this.lastCursor = cursor;
            this.lastLimit = limit;
            return page;
        }

        void setPage(CursorPage<Object> page) {
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
