package com.dt.gatepilot.apiserver.infrastructure.persistence.jdbc;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverProperties;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.GatePilotResourceStore;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 基于数据库的 GatePilot 资源存储实现。
 */
@Repository
@ConditionalOnProperty(prefix = "gatepilot.apiserver.store", name = "type", havingValue = "jdbc")
public class JdbcGatePilotResourceStore implements GatePilotResourceStore {

    private static final int MAX_LIMIT = 500;

    private final JdbcTemplate jdbcTemplate;

    private final ResourceMetadataSupport metadataSupport;

    private final ObjectMapper objectMapper;

    private final GatePilotApiserverProperties properties;

    /**
     * 创建 JDBC 资源存储。
     *
     * @param dataSource 数据源
     * @param metadataSupport metadata 工具
     * @param objectMapper JSON 转换器
     * @param properties apiserver 配置
     */
    public JdbcGatePilotResourceStore(DataSource dataSource,
                                      ResourceMetadataSupport metadataSupport,
                                      ObjectMapper objectMapper,
                                      GatePilotApiserverProperties properties) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.metadataSupport = metadataSupport;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 初始化资源表结构。
     */
    @PostConstruct
    public void initializeSchema() {
        if (!properties.getStore().getJdbc().isInitializeSchema()) {
            return;
        }
        jdbcTemplate.execute("""
                create table if not exists %s (
                    kind varchar(64) not null,
                    namespace varchar(128) not null,
                    name varchar(256) not null,
                    uid varchar(64) not null,
                    generation bigint not null,
                    resource_json text not null,
                    created_at timestamp not null,
                    updated_at timestamp not null,
                    primary key (kind, namespace, name)
                )
                """.formatted(tableName()));
    }

    @Override
    public synchronized <T> T save(ResourceKind kind, String namespace, String name, T resource, Class<T> resourceType) {
        ResourceMetadata metadata = metadataSupport.metadataOf(resource);
        Instant now = Instant.now();
        metadata.setNamespace(namespace);
        metadata.setName(name);
        if (!StringUtils.hasText(metadata.getUid())) {
            metadata.setUid(UUID.randomUUID().toString());
        }
        if (metadata.getCreatedAt() == null) {
            metadata.setCreatedAt(now);
        }
        metadata.setUpdatedAt(now);
        long generation = nextGeneration(kind, namespace, name, metadata);
        metadata.setGeneration(generation);
        upsert(kind, namespace, name, metadata, resource);
        return resource;
    }

    @Override
    public <T> Optional<T> find(ResourceKind kind, String namespace, String name, Class<T> resourceType) {
        List<T> items = jdbcTemplate.query("""
                        select resource_json from %s
                        where kind = ? and namespace = ? and name = ?
                        """.formatted(tableName()),
                (resultSet, rowNum) -> readResource(resultSet, resourceType),
                kind.name(),
                namespace,
                name);
        return items.stream().findFirst();
    }

    @Override
    public <T> CursorPage<T> list(ResourceKind kind,
                                          String namespace,
                                          String cursor,
                                          int limit,
                                          Class<T> resourceType) {
        int effectiveLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String effectiveNamespace = normalizeNamespace(namespace);
        String effectiveCursor = cursorValue(cursor);
        List<T> items = jdbcTemplate.query("""
                        select resource_json from %s
                        where kind = ?
                          and (? = '' or namespace = ?)
                          and concat(namespace, '/', name) > ?
                        order by namespace asc, name asc
                        limit ?
                        """.formatted(tableName()),
                (resultSet, rowNum) -> readResource(resultSet, resourceType),
                kind.name(),
                effectiveNamespace,
                effectiveNamespace,
                effectiveCursor,
                effectiveLimit + 1);
        String nextCursor = null;
        if (items.size() > effectiveLimit) {
            T nextItem = items.remove(effectiveLimit);
            ResourceMetadata metadata = metadataSupport.metadataOf(nextItem);
            nextCursor = metadata.getNamespace() + "/" + metadata.getName();
        }
        CursorPage<T> page = new CursorPage<>();
        page.setItems(items);
        page.setLimit(effectiveLimit);
        page.setTotal(count(kind, namespace));
        page.setNextCursor(nextCursor);
        return page;
    }

    @Override
    public synchronized void delete(ResourceKind kind, String namespace, String name) {
        jdbcTemplate.update("""
                        delete from %s
                        where kind = ? and namespace = ? and name = ?
                        """.formatted(tableName()),
                kind.name(),
                namespace,
                name);
    }

    private long nextGeneration(ResourceKind kind, String namespace, String name, ResourceMetadata metadata) {
        Long oldGeneration = jdbcTemplate.query("""
                        select generation from %s
                        where kind = ? and namespace = ? and name = ?
                        """.formatted(tableName()),
                resultSet -> resultSet.next() ? resultSet.getLong("generation") : null,
                kind.name(),
                namespace,
                name);
        if (oldGeneration == null) {
            return Optional.ofNullable(metadata.getGeneration()).orElse(1L);
        }
        return oldGeneration + 1L;
    }

    private void upsert(ResourceKind kind, String namespace, String name, ResourceMetadata metadata, Object resource) {
        int updated = jdbcTemplate.update("""
                        update %s
                        set uid = ?,
                            generation = ?,
                            resource_json = ?,
                            updated_at = ?
                        where kind = ? and namespace = ? and name = ?
                        """.formatted(tableName()),
                metadata.getUid(),
                metadata.getGeneration(),
                writeResource(resource),
                Timestamp.from(metadata.getUpdatedAt()),
                kind.name(),
                namespace,
                name);
        if (updated > 0) {
            return;
        }
        jdbcTemplate.update("""
                        insert into %s(kind, namespace, name, uid, generation, resource_json, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """.formatted(tableName()),
                kind.name(),
                namespace,
                name,
                metadata.getUid(),
                metadata.getGeneration(),
                writeResource(resource),
                Timestamp.from(metadata.getCreatedAt()),
                Timestamp.from(metadata.getUpdatedAt()));
    }

    private <T> T readResource(ResultSet resultSet, Class<T> resourceType) throws SQLException {
        try {
            return objectMapper.readValue(resultSet.getString("resource_json"), resourceType);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.ERROR.code(), "resource json deserialization failed", exception);
        }
    }

    private String writeResource(Object resource) {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.ERROR.code(), "resource json serialization failed", exception);
        }
    }

    private int count(ResourceKind kind, String namespace) {
        String effectiveNamespace = normalizeNamespace(namespace);
        Integer total = jdbcTemplate.queryForObject("""
                        select count(1) from %s
                        where kind = ? and (? = '' or namespace = ?)
                        """.formatted(tableName()),
                Integer.class,
                kind.name(),
                effectiveNamespace,
                effectiveNamespace);
        return Optional.ofNullable(total).orElse(0);
    }

    private String normalizeNamespace(String namespace) {
        return StringUtils.hasText(namespace) ? namespace : "";
    }

    private String cursorValue(String cursor) {
        return StringUtils.hasText(cursor) ? cursor : "";
    }

    private String tableName() {
        String tableName = properties.getStore().getJdbc().getTableName();
        if (!StringUtils.hasText(tableName) || !tableName.matches("[a-zA-Z0-9_]+")) {
            throw new BusinessException(CommonErrorCode.ERROR.code(), "invalid gatepilot resource table name");
        }
        return tableName;
    }
}
