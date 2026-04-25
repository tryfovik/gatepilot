package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus.audit;

import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditConstants;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditStore;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 基于 MyBatis-Plus 的运行审计存储。
 */
@Repository
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_DATABASE)
public class MybatisPlusRuntimeAuditStore implements RuntimeAuditStore {

    /**
     * 运行审计 Mapper。
     */
    private final RuntimeAuditMapper runtimeAuditMapper;

    /**
     * 创建 MyBatis-Plus 运行审计存储。
     *
     * @param runtimeAuditMapper 运行审计 Mapper
     */
    public MybatisPlusRuntimeAuditStore(RuntimeAuditMapper runtimeAuditMapper) {
        this.runtimeAuditMapper = runtimeAuditMapper;
    }

    @Override
    public void saveBatch(List<RuntimeAuditRecord> records) {
        for (RuntimeAuditRecord record : records) {
            // 批量入口保持简单，后续按吞吐改成 MyBatis 批处理
            runtimeAuditMapper.insert(entity(record));
        }
    }

    @Override
    public CursorPage<RuntimeAuditRecord> list(RuntimeAuditQuery query) {
        int effectiveLimit = effectiveLimit(query.getLimit());
        List<RuntimeAuditRecord> records = runtimeAuditMapper.selectPageByCursor(
                        query, cursorValue(query.getCursor()), effectiveLimit + 1)
                .stream()
                .map(this::record)
                .toList();
        CursorPage<RuntimeAuditRecord> page = new CursorPage<>();
        page.setItems(trimItems(records, effectiveLimit));
        page.setLimit(effectiveLimit);
        page.setTotal(runtimeAuditMapper.countByQuery(query));
        page.setNextCursor(nextCursor(records, effectiveLimit));
        return page;
    }

    /**
     * 转换为数据库实体。
     *
     * @param record 审计记录
     * @return 数据库实体
     */
    private RuntimeAuditEntity entity(RuntimeAuditRecord record) {
        RuntimeAuditEntity entity = new RuntimeAuditEntity();
        entity.setNamespace(record.getNamespace());
        entity.setNodeId(record.getNodeId());
        entity.setTraceId(record.getTraceId());
        entity.setClientIp(record.getClientIp());
        entity.setMethod(record.getMethod());
        entity.setPath(record.getPath());
        entity.setHost(record.getHost());
        entity.setRouteId(record.getRouteId());
        entity.setUpstreamName(record.getUpstreamName());
        entity.setUpstreamUri(record.getUpstreamUri());
        entity.setStatus(record.getStatus());
        entity.setLatencyMillis(record.getLatencyMillis());
        entity.setTrafficColor(record.getTrafficColor());
        entity.setMethodAllowed(record.getMethodAllowed());
        entity.setAuthenticationRequired(record.getAuthenticationRequired());
        entity.setFallback(record.getFallback());
        entity.setOutcome(record.getOutcome());
        entity.setReason(record.getReason());
        entity.setError(record.getError());
        entity.setOccurredAt(toLocalDateTime(record.getOccurredAt()));
        return entity;
    }

    /**
     * 转换为审计记录。
     *
     * @param entity 数据库实体
     * @return 审计记录
     */
    private RuntimeAuditRecord record(RuntimeAuditEntity entity) {
        RuntimeAuditRecord record = new RuntimeAuditRecord();
        record.setId(entity.getId());
        record.setNamespace(entity.getNamespace());
        record.setNodeId(entity.getNodeId());
        record.setTraceId(entity.getTraceId());
        record.setClientIp(entity.getClientIp());
        record.setMethod(entity.getMethod());
        record.setPath(entity.getPath());
        record.setHost(entity.getHost());
        record.setRouteId(entity.getRouteId());
        record.setUpstreamName(entity.getUpstreamName());
        record.setUpstreamUri(entity.getUpstreamUri());
        record.setStatus(entity.getStatus());
        record.setLatencyMillis(entity.getLatencyMillis());
        record.setTrafficColor(entity.getTrafficColor());
        record.setMethodAllowed(entity.getMethodAllowed());
        record.setAuthenticationRequired(entity.getAuthenticationRequired());
        record.setFallback(entity.getFallback());
        record.setOutcome(entity.getOutcome());
        record.setReason(entity.getReason());
        record.setError(entity.getError());
        record.setOccurredAt(toInstant(entity.getOccurredAt()));
        return record;
    }

    /**
     * 裁剪分页结果。
     *
     * @param records 多取一条的结果
     * @param limit 分页大小
     * @return 当前页结果
     */
    private List<RuntimeAuditRecord> trimItems(List<RuntimeAuditRecord> records, int limit) {
        if (records.size() <= limit) {
            return records;
        }
        return records.subList(0, limit);
    }

    /**
     * 计算下一页游标。
     *
     * @param records 多取一条的结果
     * @param limit 分页大小
     * @return 下一页游标
     */
    private String nextCursor(List<RuntimeAuditRecord> records, int limit) {
        if (records.size() <= limit) {
            return null;
        }
        return String.valueOf(records.get(limit - 1).getId());
    }

    /**
     * 计算分页大小。
     *
     * @param limit 请求分页大小
     * @return 有效分页大小
     */
    private int effectiveLimit(int limit) {
        int requestedLimit = limit <= 0 ? RuntimeAuditConstants.DEFAULT_LIMIT : limit;
        return Math.max(1, Math.min(requestedLimit, RuntimeAuditConstants.MAX_LIMIT));
    }

    /**
     * 解析游标。
     *
     * @param cursor 游标
     * @return 游标值
     */
    private long cursorValue(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return RuntimeAuditConstants.EMPTY_CURSOR;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException exception) {
            return RuntimeAuditConstants.EMPTY_CURSOR;
        }
    }

    /**
     * 转换为数据库时间。
     *
     * @param instant 时间点
     * @return 数据库时间
     */
    private LocalDateTime toLocalDateTime(Instant instant) {
        Instant effectiveInstant = instant == null ? Instant.now() : instant;
        return LocalDateTime.ofInstant(effectiveInstant, ZoneOffset.UTC);
    }

    /**
     * 转换为资源时间。
     *
     * @param localDateTime 数据库时间
     * @return 资源时间
     */
    private Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}
