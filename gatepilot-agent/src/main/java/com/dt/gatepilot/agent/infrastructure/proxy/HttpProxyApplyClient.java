package com.dt.gatepilot.agent.infrastructure.proxy;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.getboot.exception.api.exception.BusinessException;
import com.getboot.web.api.response.ApiResponse;
import java.time.Instant;
import lombok.Data;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 基于 HTTP 的 proxy apply 客户端
 */
public class HttpProxyApplyClient implements ProxyApplyClient {

    private final WebClient webClient;

    /**
     * 创建 proxy apply 客户端
     *
     * @param builder WebClient 构建器
     * @param properties agent 配置
     */
    public HttpProxyApplyClient(WebClient.Builder builder, GatePilotAgentProperties properties) {
        this.webClient = builder.baseUrl(properties.getProxyBaseUrl()).build();
    }

    @Override
    public AgentApplyResult apply(PublishedConfig config) {
        ApiResponse<ProxyApplyResponse> response = webClient.post()
                .uri(AgentProxyApiPaths.API_PREFIX + AgentProxyApiPaths.APPLY_CONFIG)
                .bodyValue(config)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProxyApplyResponse>>() {
                })
                .block();
        if (response == null) {
            throw BusinessException.of(ApiResponse.SYSTEM_ERROR_CODE, "proxy returned empty response");
        }
        if (!response.isSuccess()) {
            throw BusinessException.of(response.getCode(), response.getMessage());
        }
        if (response.getData() == null) {
            throw BusinessException.of(ApiResponse.SYSTEM_ERROR_CODE, "proxy returned empty apply result");
        }
        return response.getData().toAgentResult();
    }

    /**
     * proxy 配置应用响应
     */
    @Data
    private static class ProxyApplyResponse {

        /**
         * 应用版本
         */
        private String version;

        /**
         * 配置哈希
         */
        private String configHash;

        /**
         * 应用状态
         */
        private ConfigApplyState state;

        /**
         * 开始时间
         */
        private Instant startedAt;

        /**
         * 完成时间
         */
        private Instant finishedAt;

        /**
         * 失败原因
         */
        private String reason;

        /**
         * 状态说明
         */
        private String message;

        private AgentApplyResult toAgentResult() {
            AgentApplyResult result = new AgentApplyResult();
            // agent 上报模型和 proxy apply 响应保持字段一一映射
            result.setVersion(version);
            result.setConfigHash(configHash);
            result.setState(state);
            result.setStartedAt(startedAt);
            result.setFinishedAt(finishedAt);
            result.setReason(reason);
            result.setMessage(message);
            return result;
        }
    }
}
