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
package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;

/**
 * proxy 本地上游端点健康状态表。
 */
public class UpstreamEndpointHealthRegistry {

    /**
     * 端点健康状态。
     */
    private final ConcurrentMap<String, EndpointHealthState> states = new ConcurrentHashMap<>();

    /**
     * 判断端点是否可选。
     *
     * @param upstreamName 上游名称
     * @param endpoint 上游端点
     * @return 是否可选
     */
    public boolean selectable(String upstreamName, CompiledUpstream.CompiledEndpoint endpoint) {
        EndpointHealthState state = states.get(key(upstreamName, endpoint));
        return state == null || state.isHealthy();
    }

    /**
     * 记录一次成功探测。
     *
     * @param upstream 上游
     * @param endpoint 上游端点
     */
    public void recordSuccess(CompiledUpstream upstream, CompiledUpstream.CompiledEndpoint endpoint) {
        states.compute(key(upstream.getName(), endpoint), (ignored, state) -> {
            EndpointHealthState next = state == null ? new EndpointHealthState() : state;
            // 连续成功达到阈值后恢复端点
            next.setConsecutiveSuccess(next.getConsecutiveSuccess() + 1);
            next.setConsecutiveFailure(0);
            next.setLastCheckedAt(Instant.now());
            next.setMessage(null);
            if (next.getConsecutiveSuccess() >= healthyThreshold(upstream.getHealthCheck())) {
                next.setHealthy(true);
            }
            return next;
        });
    }

    /**
     * 记录一次失败探测。
     *
     * @param upstream 上游
     * @param endpoint 上游端点
     * @param message 失败说明
     */
    public void recordFailure(CompiledUpstream upstream,
                              CompiledUpstream.CompiledEndpoint endpoint,
                              String message) {
        states.compute(key(upstream.getName(), endpoint), (ignored, state) -> {
            EndpointHealthState next = state == null ? new EndpointHealthState() : state;
            // 连续失败达到阈值后摘除端点
            next.setConsecutiveFailure(next.getConsecutiveFailure() + 1);
            next.setConsecutiveSuccess(0);
            next.setLastCheckedAt(Instant.now());
            next.setMessage(message);
            if (next.getConsecutiveFailure() >= unhealthyThreshold(upstream.getHealthCheck())) {
                next.setHealthy(false);
            }
            return next;
        });
    }

    /**
     * 生成上游健康摘要。
     *
     * @param runtime 当前运行态
     * @return 上游健康摘要
     */
    public List<GatewayNodeStatus.UpstreamHealth> snapshot(CompiledProxyRuntime runtime) {
        return snapshot(runtime, new StaticUpstreamDiscoveryRegistry());
    }

    /**
     * 生成上游健康摘要。
     *
     * @param runtime 当前运行态
     * @param discoveryRegistry 上游服务发现注册表
     * @return 上游健康摘要
     */
    public List<GatewayNodeStatus.UpstreamHealth> snapshot(CompiledProxyRuntime runtime,
                                                           UpstreamDiscoveryRegistry discoveryRegistry) {
        if (runtime == null) {
            return List.of();
        }
        List<GatewayNodeStatus.UpstreamHealth> result = new ArrayList<>();
        for (CompiledUpstream upstream : runtime.getUpstreamsByName().values()) {
            GatewayNodeStatus.UpstreamHealth health = new GatewayNodeStatus.UpstreamHealth();
            health.setUpstreamName(upstream.getName());
            int healthy = 0;
            int unhealthy = 0;
            for (CompiledUpstream.CompiledEndpoint endpoint : discoveryRegistry.instances(upstream)) {
                if (selectable(upstream.getName(), endpoint)) {
                    healthy++;
                } else {
                    unhealthy++;
                }
            }
            health.setHealthyEndpointCount(healthy);
            health.setUnhealthyEndpointCount(unhealthy);
            result.add(health);
        }
        return result;
    }

    private String key(String upstreamName, CompiledUpstream.CompiledEndpoint endpoint) {
        return upstreamName
                + ProxyUpstreamHealthConstants.ENDPOINT_KEY_SEPARATOR
                + endpoint.getHost()
                + ProxyUpstreamHealthConstants.ENDPOINT_KEY_SEPARATOR
                + endpoint.getPort();
    }

    private int healthyThreshold(CompiledUpstream.CompiledHealthCheck healthCheck) {
        Integer threshold = healthCheck == null ? null : healthCheck.getHealthyThreshold();
        return threshold == null || threshold <= 0
                ? ProxyUpstreamHealthConstants.DEFAULT_HEALTHY_THRESHOLD
                : threshold;
    }

    private int unhealthyThreshold(CompiledUpstream.CompiledHealthCheck healthCheck) {
        Integer threshold = healthCheck == null ? null : healthCheck.getUnhealthyThreshold();
        return threshold == null || threshold <= 0
                ? ProxyUpstreamHealthConstants.DEFAULT_UNHEALTHY_THRESHOLD
                : threshold;
    }

    /**
     * 单个端点健康状态。
     */
    @Data
    private static class EndpointHealthState {

        /**
         * 是否健康。
         */
        private boolean healthy = true;

        /**
         * 连续成功次数。
         */
        private int consecutiveSuccess;

        /**
         * 连续失败次数。
         */
        private int consecutiveFailure;

        /**
         * 最近探测时间。
         */
        private Instant lastCheckedAt;

        /**
         * 最近失败说明。
         */
        private String message;
    }
}
