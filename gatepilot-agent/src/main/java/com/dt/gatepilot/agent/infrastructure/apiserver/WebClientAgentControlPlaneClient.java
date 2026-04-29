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
package com.dt.gatepilot.agent.infrastructure.apiserver;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.infrastructure.config.AgentRuntimeConstants;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.agent.infrastructure.http.AgentWebClients;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.getboot.exception.api.exception.BusinessException;
import com.getboot.web.api.response.ApiResponse;
import java.util.Optional;
import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 基于 WebClient 的 apiserver 客户端。
 *
 * <p>复用普通 HTTP filter，但不走 Spring Cloud LoadBalancer。</p>
 */
public class WebClientAgentControlPlaneClient implements AgentControlPlaneClient {

    private final WebClient webClient;

    /**
     * 创建 apiserver 客户端。
     *
     * @param builder WebClient 构建器
     * @param properties agent 配置
     */
    public WebClientAgentControlPlaneClient(WebClient.Builder builder, GatePilotAgentProperties properties) {
        this.webClient = AgentWebClients.httpClient(builder, properties.getApiserverBaseUrl());
    }

    @Override
    public void register(AgentNodeProfile profile) {
        post(AgentControlPlaneApiPaths.REGISTER, profile, new ParameterizedTypeReference<ApiResponse<GatewayNode>>() {
        });
    }

    @Override
    public void heartbeat(AgentHeartbeatSnapshot heartbeat) {
        post(AgentControlPlaneApiPaths.HEARTBEAT, heartbeat,
                new ParameterizedTypeReference<ApiResponse<GatewayNode>>() {
        });
    }

    @Override
    public Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor) {
        ApiResponse<ControlPlaneConfigPullResponse> response = post(
                AgentControlPlaneApiPaths.CONFIG_PULL,
                cursor,
                new ParameterizedTypeReference<ApiResponse<ControlPlaneConfigPullResponse>>() {
                }
        );
        ControlPlaneConfigPullResponse data = response.getData();
        if (data == null || !data.isChanged()) {
            return Optional.empty();
        }
        return Optional.ofNullable(data.getPublishedConfig());
    }

    @Override
    public void reportApplyResult(AgentApplyResult result) {
        post(AgentControlPlaneApiPaths.APPLY_RESULTS, result,
                new ParameterizedTypeReference<ApiResponse<GatewayNode>>() {
        });
    }

    @Override
    public void reportRuntimeAudits(AgentRuntimeAuditBatch batch) {
        post(AgentControlPlaneApiPaths.AUDITS, batch,
                new ParameterizedTypeReference<ApiResponse<Object>>() {
        });
    }

    private <T> ApiResponse<T> post(String path, Object body, ParameterizedTypeReference<ApiResponse<T>> type) {
        // 控制面调用是普通 HTTP 地址，不能被 LoadBalancer filter 接管
        ApiResponse<T> response = webClient.post()
                .uri(AgentControlPlaneApiPaths.API_PREFIX + path)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(type)
                .block();
        if (response == null) {
            throw BusinessException.of(ApiResponse.SYSTEM_ERROR_CODE, "apiserver returned empty response");
        }
        if (!response.isSuccess()) {
            throw BusinessException.of(response.getCode(), response.getMessage());
        }
        return response;
    }

    /**
     * apiserver 配置拉取响应。
     */
    @Data
    private static class ControlPlaneConfigPullResponse {

        /**
         * 是否存在新配置。
         */
        private boolean changed;

        /**
         * 最新已发布配置。
         */
        private PublishedConfig publishedConfig;

        /**
         * 控制面提示信息。
         */
        private String message;
    }
}
