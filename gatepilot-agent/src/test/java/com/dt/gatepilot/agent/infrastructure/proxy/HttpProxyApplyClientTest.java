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
package com.dt.gatepilot.agent.infrastructure.proxy;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.getboot.exception.api.exception.BusinessException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HTTP proxy apply 客户端测试
 */
class HttpProxyApplyClientTest {

    @Test
    void shouldPostPublishedConfigToProxyApplyPath() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        HttpProxyApplyClient client = newClient(capturedRequest,
                "{\"status\":\"success\",\"code\":200,\"message\":\"ok\",\"data\":{"
                        + "\"version\":\"v1\",\"configHash\":\"hash-v1\",\"state\":\"APPLIED\"}}");

        AgentApplyResult result = client.apply(config("v1", "hash-v1"));

        assertThat(capturedRequest.get().url().toString())
                .isEqualTo("http://proxy.test/api/gatepilot/v1/proxy/configs/apply");
        assertThat(result.getVersion()).isEqualTo("v1");
        assertThat(result.getConfigHash()).isEqualTo("hash-v1");
        assertThat(result.getState()).isEqualTo(ConfigApplyState.APPLIED);
    }

    @Test
    void shouldThrowGetbootBusinessExceptionWhenProxyResponseFailed() {
        AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();
        HttpProxyApplyClient client = newClient(capturedRequest,
                "{\"status\":\"fail\",\"code\":500,\"message\":\"proxy apply failed\",\"data\":null}");

        assertThatThrownBy(() -> client.apply(config("v1", "hash-v1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("proxy apply failed");
    }

    private HttpProxyApplyClient newClient(AtomicReference<ClientRequest> capturedRequest, String responseBody) {
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(responseBody)
                    .build());
        };
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        properties.setProxyBaseUrl("http://proxy.test");
        return new HttpProxyApplyClient(WebClient.builder().exchangeFunction(exchangeFunction), properties);
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        // 测试配置只填 proxy apply 必需字段
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
