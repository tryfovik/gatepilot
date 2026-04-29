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
package com.dt.gatepilot.proxy.infrastructure.health;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.ProxyUpstreamHealthConstants;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * proxy 上游主动健康探测器。
 */
public class UpstreamHealthProbe {

    /**
     * proxy 运行态。
     */
    private final ProxyRuntimeState runtimeState;

    /**
     * 端点健康状态表。
     */
    private final UpstreamEndpointHealthRegistry healthRegistry;

    /**
     * 上游服务发现注册表。
     */
    private final UpstreamDiscoveryRegistry upstreamDiscoveryRegistry;

    /**
     * 健康检查 HTTP 客户端。
     */
    private final WebClient webClient;

    /**
     * 创建上游健康探测器。
     *
     * @param runtimeState proxy 运行态
     * @param healthRegistry 端点健康状态表
     * @param upstreamDiscoveryRegistry 上游服务发现注册表
     * @param webClient WebClient
     */
    public UpstreamHealthProbe(ProxyRuntimeState runtimeState,
                               UpstreamEndpointHealthRegistry healthRegistry,
                               UpstreamDiscoveryRegistry upstreamDiscoveryRegistry,
                               WebClient webClient) {
        this.runtimeState = runtimeState;
        this.healthRegistry = healthRegistry;
        this.upstreamDiscoveryRegistry = upstreamDiscoveryRegistry;
        this.webClient = webClient;
    }

    /**
     * 执行一轮健康探测。
     *
     * @return 探测完成信号
     */
    public Mono<Void> probe() {
        return runtimeState.current()
                .map(this::probeRuntime)
                .orElseGet(Mono::empty);
    }

    private Mono<Void> probeRuntime(CompiledProxyRuntime runtime) {
        return Flux.fromIterable(runtime.getUpstreamsByName().values())
                .filter(this::healthCheckEnabled)
                .flatMap(upstream -> Flux.fromIterable(upstreamDiscoveryRegistry.instances(upstream))
                        .flatMap(endpoint -> probeEndpoint(upstream, endpoint)))
                .then();
    }

    private Mono<Void> probeEndpoint(CompiledUpstream upstream, CompiledUpstream.CompiledEndpoint endpoint) {
        URI healthUri = healthUri(upstream, endpoint);
        return webClient.get()
                .uri(healthUri)
                .exchangeToMono(response -> handleResponse(upstream, endpoint, response))
                .timeout(timeout(upstream.getHealthCheck()))
                .onErrorResume(exception -> {
                    healthRegistry.recordFailure(upstream, endpoint, exception.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> handleResponse(CompiledUpstream upstream,
                                      CompiledUpstream.CompiledEndpoint endpoint,
                                      ClientResponse response) {
        HttpStatusCode statusCode = response.statusCode();
        return response.releaseBody()
                .doOnSuccess(ignored -> {
                    if (statusCode.is2xxSuccessful()) {
                        healthRegistry.recordSuccess(upstream, endpoint);
                    } else {
                        healthRegistry.recordFailure(upstream, endpoint, Integer.toString(statusCode.value()));
                    }
                });
    }

    private boolean healthCheckEnabled(CompiledUpstream upstream) {
        return upstream.getHealthCheck() != null && Boolean.TRUE.equals(upstream.getHealthCheck().getEnabled());
    }

    private URI healthUri(CompiledUpstream upstream, CompiledUpstream.CompiledEndpoint endpoint) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(scheme(upstream))
                .host(endpoint.getHost())
                .path(healthPath(upstream.getHealthCheck()));
        if (endpoint.getPort() != null) {
            builder.port(endpoint.getPort());
        }
        return builder.build(true).toUri();
    }

    private String scheme(CompiledUpstream upstream) {
        Protocol protocol = upstream.getProtocol();
        if (protocol == Protocol.HTTPS || protocol == Protocol.WSS) {
            return ProxyUpstreamHealthConstants.SCHEME_HTTPS;
        }
        return ProxyUpstreamHealthConstants.SCHEME_HTTP;
    }

    private String healthPath(CompiledUpstream.CompiledHealthCheck healthCheck) {
        String path = healthCheck == null ? null : healthCheck.getPath();
        if (!StringUtils.hasText(path)) {
            return ProxyUpstreamHealthConstants.DEFAULT_HEALTH_PATH;
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private Duration timeout(CompiledUpstream.CompiledHealthCheck healthCheck) {
        Duration timeout = healthCheck == null ? null : healthCheck.getTimeout();
        return timeout == null || timeout.isNegative() || timeout.isZero()
                ? ProxyUpstreamHealthConstants.DEFAULT_TIMEOUT
                : timeout;
    }
}
