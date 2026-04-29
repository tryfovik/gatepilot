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
package com.dt.gatepilot.apiserver.domain.resource;

/**
 * apiserver 资源路径常量。
 */
public final class GatePilotResourcePaths {

    /**
     * 命名空间资源路径。
     */
    public static final String NAMESPACES = "namespaces";

    /**
     * 平台团队资源路径。
     */
    public static final String TEAMS = "teams";

    /**
     * 平台环境资源路径。
     */
    public static final String ENVIRONMENTS = "environments";

    /**
     * 配置分片资源路径。
     */
    public static final String CONFIG_SHARDS = "config-shards";

    /**
     * 隔离组资源路径。
     */
    public static final String ISOLATION_GROUPS = "isolation-groups";

    /**
     * 控制面动态参数资源路径。
     */
    public static final String CONTROL_PLANE_SETTINGS = "control-plane-settings";

    /**
     * 注册中心资源路径。
     */
    public static final String REGISTRY_CENTERS = "registry-centers";

    /**
     * 流量等级资源路径。
     */
    public static final String TRAFFIC_TIERS = "traffic-tiers";

    /**
     * 入口域名资源路径。
     */
    public static final String INGRESS_DOMAINS = "ingress-domains";

    /**
     * 项目资源路径。
     */
    public static final String PROJECTS = "projects";

    /**
     * 路由资源路径。
     */
    public static final String ROUTES = "routes";

    /**
     * 流量策略资源路径。
     */
    public static final String TRAFFIC_POLICIES = "traffic-policies";

    /**
     * 发布策略资源路径。
     */
    public static final String RELEASE_POLICIES = "release-policies";

    /**
     * 认证策略资源路径。
     */
    public static final String AUTH_POLICIES = "auth-policies";

    /**
     * 上游资源路径。
     */
    public static final String UPSTREAMS = "upstreams";

    /**
     * 已发布配置资源路径。
     */
    public static final String PUBLISHED_CONFIGS = "published-configs";

    /**
     * 配置快照资源路径。
     */
    public static final String CONFIG_SNAPSHOTS = "config-snapshots";

    /**
     * 节点资源路径。
     */
    public static final String NODES = "nodes";

    /**
     * 事件资源路径。
     */
    public static final String EVENTS = "events";

    private GatePilotResourcePaths() {
        // 资源路径常量不允许实例化
    }
}
