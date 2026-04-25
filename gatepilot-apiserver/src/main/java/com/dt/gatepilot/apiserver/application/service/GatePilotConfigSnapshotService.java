package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.apiserver.application.dto.ConfigDiffResult;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourcePaths;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.exception.api.code.CommonErrorCode;
import com.getboot.exception.api.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 配置版本快照服务，负责快照保存、查找和版本 diff。
 */
@Service
public class GatePilotConfigSnapshotService {

    private static final int LOOKUP_LIMIT = 500;

    private final GatePilotResourceService resourceService;

    private final ObjectMapper objectMapper;

    /**
     * 创建配置快照服务。
     *
     * @param resourceService 资源服务
     * @param objectMapper JSON 转换器
     */
    public GatePilotConfigSnapshotService(GatePilotResourceService resourceService, ObjectMapper objectMapper) {
        this.resourceService = resourceService;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存已发布配置的版本快照。
     *
     * @param publishedConfig 已发布配置
     * @param releaseId 发布请求标识
     * @param capturedBy 创建人
     * @param description 说明
     * @return 快照资源
     */
    public GatewayConfigSnapshot saveSnapshot(PublishedConfig publishedConfig,
                                              String releaseId,
                                              String capturedBy,
                                              String description) {
        GatewayConfigSnapshot snapshot = new GatewayConfigSnapshot();
        PublishedConfig.PublishedConfigSpec publishedSpec = publishedConfig.getSpec();
        String namespace = publishedConfig.getMetadata().getNamespace();
        String name = snapshotName(publishedSpec.getVersion(), publishedSpec.getConfigShard());
        // 快照名由版本和分片决定，方便回滚按版本查找
        snapshot.getMetadata().setName(name);
        snapshot.getMetadata().setNamespace(namespace);
        snapshot.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_VERSION, publishedSpec.getVersion());
        if (StringUtils.hasText(publishedSpec.getConfigShard())) {
            snapshot.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD,
                    publishedSpec.getConfigShard());
        }
        if (publishedSpec.getProjectRef() != null) {
            snapshot.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_PROJECT,
                    publishedSpec.getProjectRef().getName());
        }
        GatewayConfigSnapshot.GatewayConfigSnapshotSpec spec = snapshot.getSpec();
        spec.setProjectRef(publishedSpec.getProjectRef());
        spec.setPublishedConfigRef(publishedConfigRef(publishedConfig));
        spec.setReleaseId(releaseId);
        spec.setVersion(publishedSpec.getVersion());
        spec.setConfigHash(publishedSpec.getConfigHash());
        spec.setConfigShard(publishedSpec.getConfigShard());
        spec.setSequence(publishedSpec.getSequence());
        spec.setCapturedAt(Instant.now());
        spec.setCapturedBy(capturedBy);
        spec.setDescription(description);
        spec.setPublishedConfig(publishedConfig);
        GatePilotResourceType snapshotType = resourceService.requireResourceType(ResourceKind.CONFIG_SNAPSHOT);
        return (GatewayConfigSnapshot) resourceService.save(snapshotType, namespace, name, snapshot);
    }

    /**
     * 查询配置快照。
     *
     * @param namespace 命名空间
     * @param projectName 项目名称
     * @param version 版本
     * @param configShard 配置分片
     * @return 快照
     */
    public Optional<GatewayConfigSnapshot> findSnapshot(String namespace,
                                                        String projectName,
                                                        String version,
                                                        String configShard) {
        return findSnapshot(namespace, version, configShard)
                .filter(snapshot -> projectMatches(snapshot, projectName));
    }

    /**
     * 查询配置快照。
     *
     * @param namespace 命名空间
     * @param version 版本
     * @param configShard 配置分片
     * @return 快照
     */
    public Optional<GatewayConfigSnapshot> findSnapshot(String namespace, String version, String configShard) {
        CursorPage<Object> page = resourceService.list(GatePilotResourcePaths.CONFIG_SNAPSHOTS, namespace, null,
                LOOKUP_LIMIT);
        return page.getItems()
                .stream()
                .map(GatewayConfigSnapshot.class::cast)
                .filter(snapshot -> Objects.equals(snapshot.getSpec().getVersion(), version))
                .filter(snapshot -> !StringUtils.hasText(configShard)
                        || Objects.equals(snapshot.getSpec().getConfigShard(), configShard))
                .findFirst();
    }

    /**
     * 比较两个配置版本。
     *
     * @param namespace 命名空间
     * @param baseVersion 基线版本
     * @param targetVersion 目标版本
     * @param configShard 配置分片
     * @return diff 响应
     */
    public ConfigDiffResult diff(String namespace, String baseVersion, String targetVersion, String configShard) {
        GatewayConfigSnapshot base = findSnapshot(namespace, baseVersion, configShard)
                .orElseThrow(() -> notFound("基线配置快照不存在: " + baseVersion));
        GatewayConfigSnapshot target = findSnapshot(namespace, targetVersion, configShard)
                .orElseThrow(() -> notFound("目标配置快照不存在: " + targetVersion));
        PublishedConfig baseConfig = base.getSpec().getPublishedConfig();
        PublishedConfig targetConfig = target.getSpec().getPublishedConfig();
        ConfigDiffResult response = new ConfigDiffResult();
        // diff 只比较快照中的 PublishedConfig 内容
        response.setNamespace(namespace);
        response.setBaseVersion(baseVersion);
        response.setTargetVersion(targetVersion);
        response.setConfigShard(configShard);

        DiffStats routeStats = appendDiffItems(response, ConfigDiffConstants.RESOURCE_ROUTE,
                baseConfig.getSpec().getRoutes(),
                targetConfig.getSpec().getRoutes(),
                this::routeKey);
        response.setAddedRoutes(routeStats.added());
        response.setRemovedRoutes(routeStats.removed());
        response.setChangedRoutes(routeStats.changed());

        DiffStats upstreamStats = appendDiffItems(response, ConfigDiffConstants.RESOURCE_UPSTREAM,
                baseConfig.getSpec().getUpstreams(),
                targetConfig.getSpec().getUpstreams(),
                PublishedConfig.PublishedUpstream::getName);
        response.setAddedUpstreams(upstreamStats.added());
        response.setRemovedUpstreams(upstreamStats.removed());
        response.setChangedUpstreams(upstreamStats.changed());

        DiffStats policyStats = appendDiffItems(response, ConfigDiffConstants.RESOURCE_POLICY,
                baseConfig.getSpec().getPolicies(),
                targetConfig.getSpec().getPolicies(),
                this::policyKey);
        response.setAddedPolicies(policyStats.added());
        response.setRemovedPolicies(policyStats.removed());
        response.setChangedPolicies(policyStats.changed());
        response.setChanged(!response.getItems().isEmpty());
        return response;
    }

    private <T> DiffStats appendDiffItems(ConfigDiffResult response,
                                          String resourceType,
                                          List<T> baseItems,
                                          List<T> targetItems,
                                          Function<T, String> keyMapper) {
        Map<String, T> baseIndex = indexByKey(baseItems, keyMapper);
        Map<String, T> targetIndex = indexByKey(targetItems, keyMapper);
        int added = 0;
        int removed = 0;
        int changed = 0;
        for (Map.Entry<String, T> targetEntry : targetIndex.entrySet()) {
            T baseItem = baseIndex.get(targetEntry.getKey());
            String targetHash = hash(targetEntry.getValue());
            if (baseItem == null) {
                // 新版本有、旧版本没有，标记新增
                added++;
                response.getItems().add(diffItem(resourceType, targetEntry.getKey(),
                        ConfigDiffConstants.CHANGE_ADDED, null, targetHash));
                continue;
            }
            String baseHash = hash(baseItem);
            if (!Objects.equals(baseHash, targetHash)) {
                // 同名资源 hash 不一致，标记变更
                changed++;
                response.getItems().add(diffItem(resourceType, targetEntry.getKey(),
                        ConfigDiffConstants.CHANGE_CHANGED, baseHash, targetHash));
            }
        }
        for (Map.Entry<String, T> baseEntry : baseIndex.entrySet()) {
            if (!targetIndex.containsKey(baseEntry.getKey())) {
                // 旧版本有、新版本没有，标记删除
                removed++;
                response.getItems().add(diffItem(resourceType, baseEntry.getKey(), ConfigDiffConstants.CHANGE_REMOVED,
                        hash(baseEntry.getValue()), null));
            }
        }
        return new DiffStats(added, removed, changed);
    }

    private <T> Map<String, T> indexByKey(List<T> items, Function<T, String> keyMapper) {
        Map<String, T> index = new LinkedHashMap<>();
        for (T item : items) {
            // key 为空时兜底成 unknown，避免 diff 过程 NPE
            index.put(Objects.toString(keyMapper.apply(item), ConfigDiffConstants.UNKNOWN_NAME), item);
        }
        return index;
    }

    private ConfigDiffResult.ConfigDiffItem diffItem(String resourceType,
                                                       String name,
                                                       String changeType,
                                                       String baseHash,
                                                       String targetHash) {
        ConfigDiffResult.ConfigDiffItem item = new ConfigDiffResult.ConfigDiffItem();
        // diff item 只保留摘要，详情后续按需再查询快照
        item.setResourceType(resourceType);
        item.setName(name);
        item.setChangeType(changeType);
        item.setBaseHash(baseHash);
        item.setTargetHash(targetHash);
        return item;
    }

    private String routeKey(PublishedConfig.PublishedRoute route) {
        ResourceReference sourceRef = route.getSourceRef();
        if (sourceRef != null && StringUtils.hasText(sourceRef.getName())) {
            // 优先使用来源资源名，避免 routeId 变化影响 diff
            return sourceRef.getNamespace() + ConfigDiffConstants.RESOURCE_KEY_SEPARATOR + sourceRef.getName();
        }
        return route.getRouteId();
    }

    private String policyKey(PublishedConfig.PublishedPolicy policy) {
        // 策略名需要带类型，避免不同策略同名冲突
        return policy.getType() + ConfigDiffConstants.RESOURCE_KEY_SEPARATOR + policy.getName();
    }

    private String hash(Object value) {
        try {
            // 用规范 JSON 生成摘要，避免对象引用差异影响比较
            MessageDigest digest = MessageDigest.getInstance(ConfigDiffConstants.DIGEST_SHA_256);
            String json = objectMapper.writeValueAsString(value);
            return java.util.HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new BusinessException(CommonErrorCode.ERROR.code(), "配置 diff 生成失败", exception);
        }
    }

    private ResourceReference publishedConfigRef(PublishedConfig publishedConfig) {
        ResourceReference reference = new ResourceReference();
        // 快照保留 PublishedConfig 引用，便于控制台串联展示
        reference.setKind(ResourceKind.PUBLISHED_CONFIG);
        reference.setNamespace(publishedConfig.getMetadata().getNamespace());
        reference.setName(publishedConfig.getMetadata().getName());
        reference.setUid(publishedConfig.getMetadata().getUid());
        return reference;
    }

    private String snapshotName(String version, String configShard) {
        if (!StringUtils.hasText(configShard)) {
            return version;
        }
        // 资源名只能保留安全字符
        return (version + ConfigDiffConstants.SNAPSHOT_NAME_SEPARATOR + configShard)
                .replaceAll(ConfigDiffConstants.SAFE_NAME_REGEX, ConfigDiffConstants.SNAPSHOT_NAME_SEPARATOR);
    }

    private boolean projectMatches(GatewayConfigSnapshot snapshot, String projectName) {
        ResourceReference projectRef = snapshot.getSpec().getProjectRef();
        // 老快照没有 projectRef 时先兼容通过
        return projectRef == null || Objects.equals(projectRef.getName(), projectName);
    }

    private BusinessException notFound(String message) {
        // 统一使用 getboot 异常能力
        return BusinessException.of(CommonErrorCode.NOT_FOUND.code(), message);
    }

    private record DiffStats(int added, int removed, int changed) {
    }
}
