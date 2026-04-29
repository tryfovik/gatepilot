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
package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 路由目录响应。
 */
@Data
public class RouteCatalogResponse {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 配置生成时间。
     */
    private Instant generatedAt;

    /**
     * 路由数量。
     */
    private int routeCount;

    /**
     * 上游数量。
     */
    private int upstreamCount;

    /**
     * 策略数量。
     */
    private int policyCount;

    /**
     * 目标节点数量。
     */
    private int targetNodeCount;

    /**
     * 路由列表。
     */
    private List<RouteView> routes = new ArrayList<>();

    /**
     * 上游列表。
     */
    private List<UpstreamView> upstreams = new ArrayList<>();

    /**
     * 策略列表。
     */
    private List<PolicyView> policies = new ArrayList<>();

    /**
     * 节点应用状态。
     */
    private List<NodeApplyView> nodeApplyResults = new ArrayList<>();

    /**
     * 路由视图。
     */
    @Data
    public static class RouteView {

        /**
         * 路由标识。
         */
        private String routeId;

        /**
         * 路由资源名称。
         */
        private String name;

        /**
         * Host 列表。
         */
        private List<String> hosts = new ArrayList<>();

        /**
         * 路径。
         */
        private String path;

        /**
         * HTTP 方法列表。
         */
        private List<String> methods = new ArrayList<>();

        /**
         * 上游名称。
         */
        private String upstreamName;

        /**
         * 上游是否存在。
         */
        private boolean upstreamAvailable;

        /**
         * 策略名称列表。
         */
        private List<String> policyNames = new ArrayList<>();

        /**
         * 是否剥离前缀。
         */
        private Boolean stripPrefix;

        /**
         * 重写路径前缀。
         */
        private String rewritePathPrefix;
    }

    /**
     * 上游视图。
     */
    @Data
    public static class UpstreamView {

        /**
         * 上游名称。
         */
        private String name;

        /**
         * 协议。
         */
        private String protocol;

        /**
         * 负载均衡策略。
         */
        private String loadBalance;

        /**
         * 端点数量。
         */
        private int endpointCount;

        /**
         * 是否启用健康检查。
         */
        private Boolean healthCheckEnabled;

        /**
         * 端点列表。
         */
        private List<EndpointView> endpoints = new ArrayList<>();
    }

    /**
     * 端点视图。
     */
    @Data
    public static class EndpointView {

        /**
         * 端点主机。
         */
        private String host;

        /**
         * 端点端口。
         */
        private Integer port;

        /**
         * 端点权重。
         */
        private Integer weight;
    }

    /**
     * 策略视图。
     */
    @Data
    public static class PolicyView {

        /**
         * 策略名称。
         */
        private String name;

        /**
         * 策略类型。
         */
        private String type;
    }

    /**
     * 节点应用状态视图。
     */
    @Data
    public static class NodeApplyView {

        /**
         * 节点标识。
         */
        private String nodeId;

        /**
         * 可用区。
         */
        private String zone;

        /**
         * 应用状态。
         */
        private String state;

        /**
         * 已应用版本。
         */
        private String appliedVersion;

        /**
         * 失败原因码。
         */
        private String reason;

        /**
         * 说明。
         */
        private String message;
    }
}
