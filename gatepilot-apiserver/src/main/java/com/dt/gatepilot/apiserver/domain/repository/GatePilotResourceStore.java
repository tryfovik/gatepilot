package com.dt.gatepilot.apiserver.domain.repository;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import java.util.Optional;

/**
 * GatePilot 资源存储扩展点。
 */
public interface GatePilotResourceStore {

    /**
     * 保存资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param resource 资源对象
     * @param resourceType 资源类型
     * @param <T> 资源对象类型
     * @return 保存后的资源对象
     */
    <T> T save(ResourceKind kind, String namespace, String name, T resource, Class<T> resourceType);

    /**
     * 查询单个资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param resourceType 资源类型
     * @param <T> 资源对象类型
     * @return 资源对象
     */
    <T> Optional<T> find(ResourceKind kind, String namespace, String name, Class<T> resourceType);

    /**
     * 游标分页查询资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param cursor 游标
     * @param limit 返回条数
     * @param resourceType 资源类型
     * @param <T> 资源对象类型
     * @return 分页结果
     */
    <T> CursorPage<T> list(ResourceKind kind, String namespace, String cursor, int limit, Class<T> resourceType);

    /**
     * 删除资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     */
    void delete(ResourceKind kind, String namespace, String name);
}
