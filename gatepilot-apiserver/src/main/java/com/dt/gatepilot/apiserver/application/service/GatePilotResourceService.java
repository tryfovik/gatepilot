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
package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.GatePilotResourceStore;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * GatePilot 声明式资源服务。
 */
@Service
@ConditionalOnGatePilotApiserverEnabled
public class GatePilotResourceService {

    private static final int DEFAULT_LIMIT = 50;

    private final GatePilotResourceRegistry resourceRegistry;

    private final GatePilotResourceStore resourceStore;

    private final ObjectMapper objectMapper;

    /**
     * 创建资源服务。
     *
     * @param resourceRegistry 资源类型注册表
     * @param resourceStore 资源存储
     * @param objectMapper JSON 转换器
     */
    public GatePilotResourceService(GatePilotResourceRegistry resourceRegistry,
                                    GatePilotResourceStore resourceStore,
                                    ObjectMapper objectMapper) {
        this.resourceRegistry = resourceRegistry;
        this.resourceStore = resourceStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存资源。
     *
     * @param resourcePath URL 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param body 请求体
     * @return 保存后的资源
     */
    public Object save(String resourcePath, String namespace, String name, JsonNode body) {
        // 入站 JSON 先转成注册表里的资源类型
        GatePilotResourceType resourceType = requireResourceType(resourcePath);
        Object resource = objectMapper.convertValue(body, resourceType.getJavaType());
        return save(resourceType, namespace, name, resource);
    }

    /**
     * 保存已构造好的资源。
     *
     * @param resourceType 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param resource 资源对象
     * @return 保存后的资源
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object save(GatePilotResourceType resourceType, String namespace, String name, Object resource) {
        // 写入统一经过资源存储端口，业务层不关心具体介质
        return resourceStore.save(resourceType.getKind(), namespace, name, resource, (Class) resourceType.getJavaType());
    }

    /**
     * 查询资源列表。
     *
     * @param resourcePath URL 资源类型
     * @param namespace 命名空间
     * @param cursor 游标
     * @param limit 返回条数
     * @return 分页结果
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CursorPage<Object> list(String resourcePath, String namespace, String cursor, Integer limit) {
        GatePilotResourceType resourceType = requireResourceType(resourcePath);
        int effectiveLimit = Optional.ofNullable(limit).orElse(DEFAULT_LIMIT);
        // 这里保持 cursor 分页，不返回全量资源
        return (CursorPage) resourceStore.list(
                resourceType.getKind(),
                namespace,
                cursor,
                effectiveLimit,
                resourceType.getJavaType()
        );
    }

    /**
     * 查询单个资源。
     *
     * @param resourcePath URL 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @return 资源对象
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object get(String resourcePath, String namespace, String name) {
        GatePilotResourceType resourceType = requireResourceType(resourcePath);
        // 资源类型决定反序列化目标，避免接口层分散判断
        Object resource = resourceStore.find(resourceType.getKind(), namespace, name, (Class) resourceType.getJavaType())
                .orElse(null);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在");
        }
        return resource;
    }

    /**
     * 删除资源。
     *
     * @param resourcePath URL 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     */
    public void delete(String resourcePath, String namespace, String name) {
        GatePilotResourceType resourceType = requireResourceType(resourcePath);
        // 删除也只按资源类型和主键定位
        resourceStore.delete(resourceType.getKind(), namespace, name);
    }

    /**
     * 按枚举查询资源类型。
     *
     * @param kind 资源类型
     * @return 资源类型
     */
    public GatePilotResourceType requireResourceType(ResourceKind kind) {
        return switch (kind) {
            case GATEWAY_NAMESPACE -> requireResourceType(GatePilotResourcePaths.NAMESPACES);
            case PLATFORM_TEAM -> requireResourceType(GatePilotResourcePaths.TEAMS);
            case PLATFORM_ENVIRONMENT -> requireResourceType(GatePilotResourcePaths.ENVIRONMENTS);
            case CONFIG_SHARD -> requireResourceType(GatePilotResourcePaths.CONFIG_SHARDS);
            case ISOLATION_GROUP -> requireResourceType(GatePilotResourcePaths.ISOLATION_GROUPS);
            case CONTROL_PLANE_SETTING -> requireResourceType(GatePilotResourcePaths.CONTROL_PLANE_SETTINGS);
            case REGISTRY_CENTER -> requireResourceType(GatePilotResourcePaths.REGISTRY_CENTERS);
            case TRAFFIC_TIER -> requireResourceType(GatePilotResourcePaths.TRAFFIC_TIERS);
            case INGRESS_DOMAIN -> requireResourceType(GatePilotResourcePaths.INGRESS_DOMAINS);
            case GATEWAY_PROJECT -> requireResourceType(GatePilotResourcePaths.PROJECTS);
            case GATEWAY_ROUTE -> requireResourceType(GatePilotResourcePaths.ROUTES);
            case TRAFFIC_POLICY -> requireResourceType(GatePilotResourcePaths.TRAFFIC_POLICIES);
            case RELEASE_POLICY -> requireResourceType(GatePilotResourcePaths.RELEASE_POLICIES);
            case AUTH_POLICY -> requireResourceType(GatePilotResourcePaths.AUTH_POLICIES);
            case UPSTREAM -> requireResourceType(GatePilotResourcePaths.UPSTREAMS);
            case PUBLISHED_CONFIG -> requireResourceType(GatePilotResourcePaths.PUBLISHED_CONFIGS);
            case CONFIG_SNAPSHOT -> requireResourceType(GatePilotResourcePaths.CONFIG_SNAPSHOTS);
            case GATEWAY_NODE -> requireResourceType(GatePilotResourcePaths.NODES);
            case GATEWAY_EVENT -> requireResourceType(GatePilotResourcePaths.EVENTS);
        };
    }

    private GatePilotResourceType requireResourceType(String resourcePath) {
        // 未注册资源直接拒绝，避免控制面写入未知结构
        return resourceRegistry.findByPath(resourcePath)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未知资源类型: " + resourcePath));
    }
}
