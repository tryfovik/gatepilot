package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.GatePilotResourceStore;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
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
            case GATEWAY_PROJECT -> requireResourceType("projects");
            case GATEWAY_ROUTE -> requireResourceType("routes");
            case TRAFFIC_POLICY -> requireResourceType("traffic-policies");
            case RELEASE_POLICY -> requireResourceType("release-policies");
            case AUTH_POLICY -> requireResourceType("auth-policies");
            case UPSTREAM -> requireResourceType("upstreams");
            case PUBLISHED_CONFIG -> requireResourceType("published-configs");
            case CONFIG_SNAPSHOT -> requireResourceType("config-snapshots");
            case GATEWAY_NODE -> requireResourceType("nodes");
            case GATEWAY_EVENT -> requireResourceType("events");
        };
    }

    private GatePilotResourceType requireResourceType(String resourcePath) {
        return resourceRegistry.findByPath(resourcePath)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未知资源类型: " + resourcePath));
    }
}
