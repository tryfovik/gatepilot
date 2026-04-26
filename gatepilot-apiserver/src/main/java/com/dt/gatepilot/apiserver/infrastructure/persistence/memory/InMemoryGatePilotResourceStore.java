package com.dt.gatepilot.apiserver.infrastructure.persistence.memory;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.GatePilotResourceStore;
import com.dt.gatepilot.apiserver.domain.repository.ResourceStoreConstants;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 内存版资源存储，仅用于开发期和单机验证，生产环境需要替换为持久化实现。
 */
@Repository
@ConditionalOnGatePilotApiserverEnabled
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_MEMORY,
        matchIfMissing = true)
public class InMemoryGatePilotResourceStore implements GatePilotResourceStore {

    private static final int MAX_LIMIT = 500;

    private final ResourceMetadataSupport metadataSupport;

    private final Map<ResourceKind, Map<ResourceStoreKey, Object>> resources = new ConcurrentHashMap<>();

    /**
     * 创建内存资源存储。
     *
     * @param metadataSupport metadata 工具
     */
    public InMemoryGatePilotResourceStore(ResourceMetadataSupport metadataSupport) {
        this.metadataSupport = metadataSupport;
    }

    @Override
    public synchronized <T> T save(ResourceKind kind, String namespace, String name, T resource, Class<T> resourceType) {
        ResourceStoreKey key = new ResourceStoreKey(namespace, name);
        Map<ResourceStoreKey, Object> resourcesByKind = resources.computeIfAbsent(kind, ignored -> new LinkedHashMap<>());
        Object oldResource = resourcesByKind.get(key);
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
        metadata.setGeneration(nextGeneration(oldResource, metadata));
        resourcesByKind.put(key, resource);
        return resource;
    }

    @Override
    public <T> Optional<T> find(ResourceKind kind, String namespace, String name, Class<T> resourceType) {
        return Optional.ofNullable(resources.getOrDefault(kind, Map.of()).get(new ResourceStoreKey(namespace, name)))
                .map(resourceType::cast);
    }

    @Override
    public <T> CursorPage<T> list(ResourceKind kind,
                                          String namespace,
                                          String cursor,
                                          int limit,
                                          Class<T> resourceType) {
        int effectiveLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        List<Map.Entry<ResourceStoreKey, Object>> matched = resources.getOrDefault(kind, Map.of())
                .entrySet()
                .stream()
                .filter(entry -> !StringUtils.hasText(namespace) || namespace.equals(entry.getKey().namespace()))
                .sorted(Comparator.comparing(entry -> entry.getKey().cursor()))
                .toList();
        List<T> items = new ArrayList<>();
        boolean afterCursor = !StringUtils.hasText(cursor);
        String nextCursor = null;
        for (Map.Entry<ResourceStoreKey, Object> entry : matched) {
            String currentCursor = entry.getKey().cursor();
            if (!afterCursor) {
                afterCursor = currentCursor.equals(cursor);
                continue;
            }
            if (items.size() >= effectiveLimit) {
                nextCursor = cursorOfLastItem(items);
                break;
            }
            items.add(resourceType.cast(entry.getValue()));
        }
        CursorPage<T> page = new CursorPage<>();
        page.setItems(items);
        page.setLimit(effectiveLimit);
        page.setTotal(matched.size());
        page.setNextCursor(nextCursor);
        return page;
    }

    private <T> String cursorOfLastItem(List<T> items) {
        ResourceMetadata metadata = metadataSupport.metadataOf(items.get(items.size() - 1));
        return metadata.getNamespace() + ResourceStoreConstants.CURSOR_SEPARATOR + metadata.getName();
    }

    @Override
    public synchronized void delete(ResourceKind kind, String namespace, String name) {
        resources.getOrDefault(kind, Map.of()).remove(new ResourceStoreKey(namespace, name));
    }

    private long nextGeneration(Object oldResource, ResourceMetadata metadata) {
        if (oldResource == null) {
            return Optional.ofNullable(metadata.getGeneration()).orElse(1L);
        }
        ResourceMetadata oldMetadata = metadataSupport.metadataOf(oldResource);
        return Optional.ofNullable(oldMetadata.getGeneration()).orElse(0L) + 1L;
    }

    /**
     * 资源存储键。
     */
    private record ResourceStoreKey(String namespace, String name) {

        /**
         * 生成游标。
         *
         * @return 游标
         */
        String cursor() {
            return namespace + ResourceStoreConstants.CURSOR_SEPARATOR + name;
        }
    }
}
