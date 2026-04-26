package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.GatePilotResourceStore;
import com.dt.gatepilot.apiserver.domain.repository.ResourceStoreConstants;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 基于 MyBatis-Plus 的 GatePilot 资源存储实现。
 */
@Repository
@ConditionalOnGatePilotApiserverEnabled
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_DATABASE)
public class MybatisPlusGatePilotResourceStore implements GatePilotResourceStore {

    private final GatePilotResourceMapper resourceMapper;

    private final ResourceMetadataSupport metadataSupport;

    private final ObjectMapper objectMapper;

    /**
     * 创建 MyBatis-Plus 资源存储。
     *
     * @param resourceMapper 资源 Mapper
     * @param metadataSupport metadata 工具
     * @param objectMapper JSON 转换器
     */
    public MybatisPlusGatePilotResourceStore(GatePilotResourceMapper resourceMapper,
                                             ResourceMetadataSupport metadataSupport,
                                             ObjectMapper objectMapper) {
        this.resourceMapper = resourceMapper;
        this.metadataSupport = metadataSupport;
        this.objectMapper = objectMapper;
    }

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
    @Override
    public <T> T save(ResourceKind kind, String namespace, String name, T resource, Class<T> resourceType) {
        ResourceMetadata metadata = metadataSupport.metadataOf(resource);
        Instant now = Instant.now();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        GatePilotResourceRecord current = resourceMapper.selectByResourceKey(kind.name(), namespace, name);
        if (current == null) {
            insertNew(kind, namespace, name, metadata, resource, now);
            return resource;
        }
        updateExisting(kind, namespace, name, metadata, resource, current, now);
        return resource;
    }

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
    @Override
    public <T> Optional<T> find(ResourceKind kind, String namespace, String name, Class<T> resourceType) {
        GatePilotResourceRecord record = resourceMapper.selectByResourceKey(kind.name(), namespace, name);
        return Optional.ofNullable(record).map(item -> readResource(item.getResourceJson(), resourceType));
    }

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
    @Override
    public <T> CursorPage<T> list(ResourceKind kind,
                                  String namespace,
                                  String cursor,
                                  int limit,
                                  Class<T> resourceType) {
        int effectiveLimit = Math.max(1, Math.min(limit, MybatisPlusResourceStoreConstants.MAX_LIMIT));
        List<T> items = resourceMapper.selectPageByCursor(
                        kind.name(),
                        normalizeNamespace(namespace),
                        cursorNamespace(cursor),
                        cursorName(cursor),
                        effectiveLimit + 1)
                .stream()
                .map(record -> readResource(record.getResourceJson(), resourceType))
                .toList();
        CursorPage<T> page = new CursorPage<>();
        page.setItems(trimItems(items, effectiveLimit));
        page.setLimit(effectiveLimit);
        page.setTotal(resourceMapper.countByKindAndNamespace(kind.name(), normalizeNamespace(namespace)));
        page.setNextCursor(nextCursor(items, effectiveLimit));
        return page;
    }

    /**
     * 删除资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     */
    @Override
    public void delete(ResourceKind kind, String namespace, String name) {
        resourceMapper.deleteByResourceKey(kind.name(), namespace, name);
    }

    /**
     * 新增资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param metadata 资源 metadata
     * @param resource 资源对象
     * @param now 当前时间
     */
    private void insertNew(ResourceKind kind,
                           String namespace,
                           String name,
                           ResourceMetadata metadata,
                           Object resource,
                           Instant now) {
        if (metadata.getGeneration() != null) {
            throw writeConflict();
        }
        // 新资源由控制面生成 uid 和第一代 generation
        if (!StringUtils.hasText(metadata.getUid())) {
            metadata.setUid(UUID.randomUUID().toString());
        }
        if (metadata.getCreatedAt() == null) {
            metadata.setCreatedAt(now);
        }
        metadata.setUpdatedAt(now);
        metadata.setGeneration(MybatisPlusResourceStoreConstants.FIRST_GENERATION);
        try {
            resourceMapper.insert(record(kind, namespace, name, metadata, resource, metadata.getGeneration()));
        } catch (DuplicateKeyException exception) {
            throw writeConflict();
        }
    }

    /**
     * 更新已有资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param metadata 资源 metadata
     * @param resource 资源对象
     * @param current 当前数据库记录
     * @param now 当前时间
     */
    private void updateExisting(ResourceKind kind,
                                String namespace,
                                String name,
                                ResourceMetadata metadata,
                                Object resource,
                                GatePilotResourceRecord current,
                                Instant now) {
        if (metadata.getGeneration() != null && !metadata.getGeneration().equals(current.getGeneration())) {
            throw writeConflict();
        }
        long nextGeneration = current.getGeneration() + MybatisPlusResourceStoreConstants.GENERATION_STEP;
        metadata.setUid(current.getUid());
        metadata.setCreatedAt(toInstant(current.getCreatedAt()));
        metadata.setUpdatedAt(now);
        metadata.setGeneration(nextGeneration);
        GatePilotResourceRecord record = record(kind, namespace, name, metadata, resource, current.getGeneration());
        record.setId(current.getId());
        // updateById 交给 getboot-database 注册的 MyBatis-Plus 乐观锁拦截器处理
        if (resourceMapper.updateById(record) == 0) {
            throw writeConflict();
        }
    }

    /**
     * 构造数据库记录。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param metadata 资源 metadata
     * @param resource 资源对象
     * @param recordGeneration 数据库记录 generation
     * @return 数据库记录
     */
    private GatePilotResourceRecord record(ResourceKind kind,
                                           String namespace,
                                           String name,
                                           ResourceMetadata metadata,
                                           Object resource,
                                           Long recordGeneration) {
        GatePilotResourceRecord record = new GatePilotResourceRecord();
        // 记录 generation 保留旧值，JSON 中保存的是新资源版本
        record.setKind(kind.name());
        record.setNamespace(namespace);
        record.setName(name);
        record.setUid(metadata.getUid());
        record.setGeneration(recordGeneration);
        record.setResourceJson(writeResource(resource));
        record.setCreatedAt(toLocalDateTime(metadata.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(metadata.getUpdatedAt()));
        return record;
    }

    /**
     * 裁剪分页结果。
     *
     * @param items 多取一条的结果
     * @param limit 分页大小
     * @param <T> 资源对象类型
     * @return 当前页资源列表
     */
    private <T> List<T> trimItems(List<T> items, int limit) {
        if (items.size() <= limit) {
            return items;
        }
        // 多取的一条只用于计算 nextCursor，不返回给调用方
        return items.subList(0, limit);
    }

    /**
     * 计算下一页游标。
     *
     * @param items 多取一条的结果
     * @param limit 分页大小
     * @param <T> 资源对象类型
     * @return 下一页游标
     */
    private <T> String nextCursor(List<T> items, int limit) {
        if (items.size() <= limit) {
            return null;
        }
        ResourceMetadata metadata = metadataSupport.metadataOf(items.get(limit - 1));
        return metadata.getNamespace() + ResourceStoreConstants.CURSOR_SEPARATOR + metadata.getName();
    }

    /**
     * 反序列化资源。
     *
     * @param resourceJson 资源 JSON
     * @param resourceType 资源类型
     * @param <T> 资源对象类型
     * @return 资源对象
     */
    private <T> T readResource(String resourceJson, Class<T> resourceType) {
        try {
            return objectMapper.readValue(resourceJson, resourceType);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.ERROR.code(),
                    MybatisPlusResourceStoreConstants.MESSAGE_JSON_DESERIALIZATION_FAILED, exception);
        }
    }

    /**
     * 序列化资源。
     *
     * @param resource 资源对象
     * @return 资源 JSON
     */
    private String writeResource(Object resource) {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.ERROR.code(),
                    MybatisPlusResourceStoreConstants.MESSAGE_JSON_SERIALIZATION_FAILED, exception);
        }
    }

    /**
     * 规范化命名空间。
     *
     * @param namespace 命名空间
     * @return 规范化命名空间
     */
    private String normalizeNamespace(String namespace) {
        return StringUtils.hasText(namespace) ? namespace : "";
    }

    /**
     * 解析游标命名空间。
     *
     * @param cursor 游标
     * @return 游标命名空间
     */
    private String cursorNamespace(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        int separatorIndex = cursor.indexOf(ResourceStoreConstants.CURSOR_SEPARATOR);
        if (separatorIndex < 0) {
            return "";
        }
        // 游标按 namespace/name 拆开，避免 SQL 对列做 concat
        return cursor.substring(0, separatorIndex);
    }

    /**
     * 解析游标资源名称。
     *
     * @param cursor 游标
     * @return 游标资源名称
     */
    private String cursorName(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        int separatorIndex = cursor.indexOf(ResourceStoreConstants.CURSOR_SEPARATOR);
        if (separatorIndex < 0) {
            return cursor;
        }
        return cursor.substring(separatorIndex + ResourceStoreConstants.CURSOR_SEPARATOR.length());
    }

    /**
     * 转换为数据库时间。
     *
     * @param instant 时间点
     * @return 数据库时间
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * 转换为资源时间。
     *
     * @param localDateTime 数据库时间
     * @return 资源时间
     */
    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.UTC);
    }

    /**
     * 创建资源写入冲突异常。
     *
     * @return 资源写入冲突异常
     */
    private BusinessException writeConflict() {
        // 统一返回 409 语义，提示调用方刷新资源后重试
        return new BusinessException(CommonErrorCode.REQUEST_PROCESSING.code(),
                MybatisPlusResourceStoreConstants.MESSAGE_RESOURCE_WRITE_CONFLICT);
    }
}
