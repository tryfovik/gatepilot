package com.dt.platform.gateway.infrastructure.health;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
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

    private final WebClient webClient;

    private final GatewayProperties properties;

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    /**
     * 创建上游健康检查器。
     *
     * @param webClient HTTP 客户端
     * @param properties 网关配置
     * @param routeDefinitionLocator 路由定义定位器
     */
    public UpstreamHealthIndicator(WebClient webClient,
                                   GatewayProperties properties,
                                   GatewayRouteDefinitionLocator routeDefinitionLocator) {
        this.webClient = webClient;
        this.properties = properties;
        this.routeDefinitionLocator = routeDefinitionLocator;
    }

    @Override
    public Mono<Health> health() {
        return Flux.fromIterable(routeDefinitionLocator.getActuatorRoutes())
                .concatMap(definition -> checkUpstream(definition)
                        .map(result -> Map.entry(definition.getProjectKey() + "/" + definition.getRouteKey(), result)))
                .collectList()
                .map(this::buildHealth);
    }

    private Mono<UpstreamHealthResult> checkUpstream(GatewayRouteDefinition definition) {
        if (definition.getActuatorUri() == null) {
            return Mono.just(UpstreamHealthResult.down(
                    definition.getProjectKey(),
                    definition.getRouteKey(),
                    null,
                    new IllegalArgumentException("gateway actuator uri must not be null")
            ));
        }
        URI targetUri = definition.getActuatorUri().resolve(properties.getHealth().getPath());
        return webClient.get()
                .uri(targetUri)
                .exchangeToMono(response -> readResponse(definition, targetUri, response.statusCode(), response))
                .timeout(properties.getHealth().getTimeout())
                .onErrorResume(exception -> Mono.just(UpstreamHealthResult.down(
                        definition.getProjectKey(),
                        definition.getRouteKey(),
                        targetUri,
                        exception
                )));
    }

    private Mono<UpstreamHealthResult> readResponse(GatewayRouteDefinition definition,
                                                    URI targetUri,
                                                    HttpStatusCode statusCode,
                                                    org.springframework.web.reactive.function.client.ClientResponse response) {
        return response.bodyToMono(Map.class)
                .defaultIfEmpty(Map.of())
                .map(body -> UpstreamHealthResult.fromResponse(
                        definition.getProjectKey(),
                        definition.getRouteKey(),
                        targetUri,
                        statusCode,
                        body
                ));
    }

    private Health buildHealth(List<Map.Entry<String, UpstreamHealthResult>> results) {
        if (results.isEmpty()) {
            return Health.unknown().withDetail("message", "no actuator routes configured").build();
        }
        boolean healthy = results.stream().allMatch(entry -> entry.getValue().isHealthy());
        Health.Builder builder = healthy ? Health.up() : Health.down();
        for (Map.Entry<String, UpstreamHealthResult> entry : results) {
            builder.withDetail(entry.getKey(), entry.getValue().toDetail());
        }
        return builder.build();
    }

    static final class UpstreamHealthResult {

        private final String projectKey;

        private final String routeKey;

        private final URI targetUri;

        private final String status;

        private final Integer httpStatus;

        private final String error;

        private UpstreamHealthResult(String projectKey,
                                     String routeKey,
                                     URI targetUri,
                                     String status,
                                     Integer httpStatus,
                                     String error) {
            this.projectKey = projectKey;
            this.routeKey = routeKey;
            this.targetUri = targetUri;
            this.status = status;
            this.httpStatus = httpStatus;
            this.error = error;
        }

        static UpstreamHealthResult fromResponse(String projectKey,
                                                 String routeKey,
                                                 URI targetUri,
                                                 HttpStatusCode httpStatus,
                                                 Map<?, ?> body) {
            Object status = body.get("status");
            String upstreamStatus = status == null ? "UNKNOWN" : status.toString();
            if (httpStatus.is2xxSuccessful() && "UP".equalsIgnoreCase(upstreamStatus)) {
                return new UpstreamHealthResult(projectKey, routeKey, targetUri, "UP", httpStatus.value(), null);
            }
            String errorMessage = httpStatus.is2xxSuccessful()
                    ? "upstream health status is " + upstreamStatus
                    : "upstream returned http " + httpStatus.value();
            return new UpstreamHealthResult(projectKey, routeKey, targetUri, "DOWN", httpStatus.value(), errorMessage);
        }

        static UpstreamHealthResult down(String projectKey, String routeKey, URI targetUri, Throwable exception) {
            return new UpstreamHealthResult(projectKey, routeKey, targetUri, "DOWN", null, exception.getMessage());
        }

        boolean isHealthy() {
            return "UP".equals(status);
        }

        Map<String, Object> toDetail() {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("project", projectKey);
            detail.put("route", routeKey);
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
