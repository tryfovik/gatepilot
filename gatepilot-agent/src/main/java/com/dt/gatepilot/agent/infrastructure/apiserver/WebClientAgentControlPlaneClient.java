package com.dt.gatepilot.agent.infrastructure.apiserver;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.agent.infrastructure.config.AgentRuntimeConstants;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.getboot.exception.api.exception.BusinessException;
import com.getboot.web.api.response.ApiResponse;
import java.util.Optional;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 基于 WebClient 的 apiserver 客户端。
 *
 * <p>Trace Header 透传由 getboot-http-client 自动挂载到 WebClient.Builder，不在这里手写。</p>
 */
@Component
@ConditionalOnProperty(prefix = AgentRuntimeConstants.CONFIG_PREFIX,
        name = AgentRuntimeConstants.ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
public class WebClientAgentControlPlaneClient implements AgentControlPlaneClient {

    private final WebClient webClient;

    /**
     * 创建 apiserver 客户端。
     *
     * @param builder WebClient 构建器
     * @param properties agent 配置
     */
    public WebClientAgentControlPlaneClient(WebClient.Builder builder, GatePilotAgentProperties properties) {
        this.webClient = builder.baseUrl(properties.getApiserverBaseUrl()).build();
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

    private <T> ApiResponse<T> post(String path, Object body, ParameterizedTypeReference<ApiResponse<T>> type) {
        // 统一走 Spring 管理的 WebClient，Trace 透传交给 getboot-http-client
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
