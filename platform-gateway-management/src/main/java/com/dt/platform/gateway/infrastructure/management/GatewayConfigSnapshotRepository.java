package com.dt.platform.gateway.infrastructure.management;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 保存配置版本快照和发布事件的有界内存仓库。
 */
public class GatewayConfigSnapshotRepository {

    private static final int DEFAULT_LIMIT = 50;

    private static final int MAX_LIMIT = 200;

    private static final int SNAPSHOT_CAPACITY = 200;

    private static final int RELEASE_RECORD_CAPACITY = 500;

    private final AtomicLong sequence = new AtomicLong();

    private final ConcurrentLinkedDeque<GatewayConfigSnapshotRecord> snapshots = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<GatewayConfigReleaseRecord> releaseRecords = new ConcurrentLinkedDeque<>();

    /**
     * 保存当前启动生效配置快照。
     *
     * @param summary 配置摘要
     * @param properties 生效配置
     * @return 生效配置快照
     */
    public GatewayConfigSnapshotRecord saveActiveSnapshot(GatewayManagementService.GatewayConfigSummary summary,
                                                          GatewayProperties properties) {
        GatewayConfigSnapshotRecord snapshot = new GatewayConfigSnapshotRecord(
                nextId("active"),
                "ACTIVE",
                true,
                Instant.now(),
                "system",
                "Current active gateway config loaded at startup",
                summary,
                List.of(),
                List.of(),
                List.of(),
                properties
        );
        saveSnapshot(snapshot);
        saveReleaseRecord(new GatewayConfigReleaseRecord(
                nextId("event"),
                snapshot.versionId(),
                "BOOTSTRAP_ACTIVE_CONFIG",
                "ACTIVE",
                snapshot.createdAt(),
                "system",
                snapshot.reason(),
                "Current active config snapshot is available"
        ));
        return snapshot;
    }

    /**
     * 保存候选配置快照。
     *
     * @param status 快照状态
     * @param operator 操作者
     * @param reason 原因
     * @param summary 配置摘要
     * @param errors 校验错误
     * @param warnings 校验警告
     * @param changes 配置变化
     * @param properties 候选配置
     * @return 候选配置快照
     */
    public GatewayConfigSnapshotRecord saveCandidateSnapshot(String status,
                                                             String operator,
                                                             String reason,
                                                             GatewayManagementService.GatewayConfigSummary summary,
                                                             List<String> errors,
                                                             List<String> warnings,
                                                             List<GatewayManagementService.GatewayConfigChangeView> changes,
                                                             GatewayProperties properties) {
        GatewayConfigSnapshotRecord snapshot = new GatewayConfigSnapshotRecord(
                nextId("candidate"),
                status,
                false,
                Instant.now(),
                normalizeText(operator, "unknown"),
                normalizeText(reason, "Candidate gateway config snapshot"),
                summary,
                copy(errors),
                copy(warnings),
                copy(changes),
                properties
        );
        saveSnapshot(snapshot);
        saveReleaseRecord(new GatewayConfigReleaseRecord(
                nextId("event"),
                snapshot.versionId(),
                "CREATE_CANDIDATE_SNAPSHOT",
                status,
                snapshot.createdAt(),
                snapshot.operator(),
                snapshot.reason(),
                "Candidate config snapshot status: " + status
        ));
        return snapshot;
    }

    /**
     * 查询配置快照列表。
     *
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 配置快照
     */
    public List<GatewayConfigSnapshotRecord> listSnapshots(String status, Integer limit) {
        int actualLimit = normalizeLimit(limit);
        List<GatewayConfigSnapshotRecord> result = new ArrayList<>();
        for (GatewayConfigSnapshotRecord snapshot : snapshots) {
            if (matches(snapshot.status(), status)) {
                result.add(snapshot);
                if (result.size() >= actualLimit) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * 按版本号查询配置快照。
     *
     * @param versionId 版本号
     * @return 配置快照
     */
    public Optional<GatewayConfigSnapshotRecord> findSnapshot(String versionId) {
        if (versionId == null || versionId.isBlank()) {
            return Optional.empty();
        }
        for (GatewayConfigSnapshotRecord snapshot : snapshots) {
            if (snapshot.versionId().equals(versionId)) {
                return Optional.of(snapshot);
            }
        }
        return Optional.empty();
    }

    /**
     * 查询发布事件记录。
     *
     * @param action 动作过滤
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 发布事件记录
     */
    public List<GatewayConfigReleaseRecord> listReleaseRecords(String action, String status, Integer limit) {
        int actualLimit = normalizeLimit(limit);
        List<GatewayConfigReleaseRecord> result = new ArrayList<>();
        for (GatewayConfigReleaseRecord record : releaseRecords) {
            if (matches(record.action(), action) && matches(record.status(), status)) {
                result.add(record);
                if (result.size() >= actualLimit) {
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    private void saveSnapshot(GatewayConfigSnapshotRecord snapshot) {
        snapshots.addFirst(snapshot);
        trimSnapshots();
    }

    private void saveReleaseRecord(GatewayConfigReleaseRecord record) {
        releaseRecords.addFirst(record);
        trimReleaseRecords();
    }

    private void trimSnapshots() {
        while (snapshots.size() > SNAPSHOT_CAPACITY) {
            snapshots.pollLast();
        }
    }

    private void trimReleaseRecords() {
        while (releaseRecords.size() > RELEASE_RECORD_CAPACITY) {
            releaseRecords.pollLast();
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private boolean matches(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(expected.trim());
    }

    private String normalizeText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String nextId(String prefix) {
        return prefix + "-" + Instant.now().toEpochMilli() + "-" + sequence.incrementAndGet();
    }

    /**
     * 配置版本快照。
     */
    public record GatewayConfigSnapshotRecord(String versionId,
                                              String status,
                                              boolean active,
                                              Instant createdAt,
                                              String operator,
                                              String reason,
                                              GatewayManagementService.GatewayConfigSummary summary,
                                              List<String> errors,
                                              List<String> warnings,
                                              List<GatewayManagementService.GatewayConfigChangeView> changes,
                                              GatewayProperties properties) {
    }

    /**
     * 配置发布事件记录。
     */
    public record GatewayConfigReleaseRecord(String recordId,
                                             String versionId,
                                             String action,
                                             String status,
                                             Instant createdAt,
                                             String operator,
                                             String reason,
                                             String message) {
    }
}
