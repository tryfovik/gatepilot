package com.dt.platform.gateway.infrastructure.health;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关上游健康检查器。
 */
public class UpstreamHealthIndicator implements ReactiveHealthIndicator {

    /**
     * HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 网关配置。
     */
    private final GatewayProperties properties;

    /**
     * 创建上游健康检查器。
     *
     * @param webClient HTTP 客户端
     * @param properties 网关配置
     */
    public UpstreamHealthIndicator(WebClient webClient,
                                   GatewayProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    /**
     * 汇总全部上游健康状态。
     *
     * @return 聚合健康结果
     */
    @Override
    public Mono<Health> health() {
        return Flux.fromIterable(properties.getUpstreams().entrySet())
                .concatMap(entry -> checkUpstream(entry.getKey(), entry.getValue())
                        .map(result -> Map.entry(entry.getKey(), result)))
                .collectList()
                .map(this::buildHealth);
    }

    /**
     * 检查单个上游应用健康状态。
     *
     * @param upstreamName 上游名称
     * @param upstream 上游配置
     * @return 检查结果
     */
    private Mono<UpstreamHealthResult> checkUpstream(String upstreamName,
                                                     GatewayProperties.UpstreamProperties upstream) {
        if (!upstream.isEnabled() || !upstream.isActuatorEnabled()) {
            return Mono.just(UpstreamHealthResult.disabled(upstreamName, upstream.getActuatorUri()));
        }
        if (upstream.getActuatorUri() == null) {
            return Mono.just(UpstreamHealthResult.down(
                    upstreamName,
                    null,
                    new IllegalArgumentException("gateway actuator uri must not be null")
            ));
        }
        URI targetUri = upstream.getActuatorUri().resolve(properties.getHealth().getPath());
        return webClient.get()
                .uri(targetUri)
                .exchangeToMono(response -> readResponse(upstreamName, targetUri, response.statusCode(), response))
                .timeout(properties.getHealth().getTimeout())
                .onErrorResume(exception -> Mono.just(UpstreamHealthResult.down(upstreamName, targetUri, exception)));
    }

    /**
     * 读取上游健康响应并转换为统一结果。
     *
     * @param upstreamName 上游名称
     * @param targetUri 健康探测地址
     * @param statusCode HTTP 状态码
     * @param response HTTP 响应
     * @return 检查结果
     */
    private Mono<UpstreamHealthResult> readResponse(String upstreamName,
                                                    URI targetUri,
                                                    HttpStatusCode statusCode,
                                                    org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .map(body -> UpstreamHealthResult.fromResponse(upstreamName, targetUri, statusCode, body));
    }

    /**
     * 构造聚合健康结果。
     *
     * @param results 全部上游健康结果
     * @return 聚合健康结果
     */
    private Health buildHealth(List<Map.Entry<String, UpstreamHealthResult>> results) {
        if (results.isEmpty()) {
            return Health.unknown().withDetail("message", "no upstreams configured").build();
        }
        boolean healthy = results.stream().allMatch(entry -> entry.getValue().isHealthy());
        Health.Builder builder = healthy ? Health.up() : Health.down();
        for (Map.Entry<String, UpstreamHealthResult> entry : results) {
            builder.withDetail(entry.getKey(), entry.getValue().toDetail());
        }
        return builder.build();
    }

    /**
     * 单个上游健康检查结果。
     */
    static final class UpstreamHealthResult {

        /**
         * 上游名称。
         */
        private final String name;

        /**
         * 目标地址。
         */
        private final URI targetUri;

        /**
         * 健康状态。
         */
        private final String status;

        /**
         * HTTP 状态码。
         */
        private final Integer httpStatus;

        /**
         * 错误信息。
         */
        private final String error;

        private UpstreamHealthResult(String name, URI targetUri, String status, Integer httpStatus, String error) {
            this.name = name;
            this.targetUri = targetUri;
            this.status = status;
            this.httpStatus = httpStatus;
            this.error = error;
        }

        /**
         * 根据上游响应创建检查结果。
         *
         * @param name 上游名称
         * @param targetUri 目标地址
         * @param httpStatus HTTP 状态码
         * @param body 响应体
         * @return 检查结果
         */
        static UpstreamHealthResult fromResponse(String name,
                                                 URI targetUri,
                                                 HttpStatusCode httpStatus,
                                                 Map<?, ?> body) {
            Object status = body.get("status");
            String upstreamStatus = status == null ? "UNKNOWN" : status.toString();
            if (httpStatus.is2xxSuccessful() && "UP".equalsIgnoreCase(upstreamStatus)) {
                return new UpstreamHealthResult(name, targetUri, "UP", httpStatus.value(), null);
            }
            String errorMessage = httpStatus.is2xxSuccessful()
                    ? "upstream health status is " + upstreamStatus
                    : "upstream returned http " + httpStatus.value();
            return new UpstreamHealthResult(name, targetUri, "DOWN", httpStatus.value(), errorMessage);
        }

        /**
         * 根据异常创建失败结果。
         *
         * @param name 上游名称
         * @param targetUri 目标地址
         * @param exception 异常
         * @return 检查结果
         */
        static UpstreamHealthResult down(String name, URI targetUri, Throwable exception) {
            return new UpstreamHealthResult(name, targetUri, "DOWN", null, exception.getMessage());
        }

        /**
         * 根据禁用状态创建检查结果。
         *
         * @param name 上游名称
         * @param targetUri 目标地址
         * @return 检查结果
         */
        static UpstreamHealthResult disabled(String name, URI targetUri) {
            return new UpstreamHealthResult(name, targetUri, "DISABLED", null, null);
        }

        /**
         * 判断当前结果是否可视为健康状态。
         *
         * @return 是否健康
         */
        boolean isHealthy() {
            return "UP".equals(status) || "DISABLED".equals(status);
        }

        /**
         * 转换为健康详情。
         *
         * @return 健康详情
         */
        Map<String, Object> toDetail() {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("name", name);
            detail.put("status", status);
            if (targetUri != null) {
                detail.put("uri", targetUri.toString());
            }
            if (httpStatus != null) {
                detail.put("httpStatus", httpStatus);
            }
            if (error != null && !error.isBlank()) {
                detail.put("error", error);
            }
            return detail;
        }
    }
}
