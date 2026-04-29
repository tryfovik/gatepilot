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
package com.dt.gatepilot.apiserver.infrastructure.persistence.memory;

import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditConstants;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditStore;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * 内存版运行审计存储，仅用于开发期和单机验证。
 */
@Repository
@ConditionalOnGatePilotApiserverEnabled
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_MEMORY,
        matchIfMissing = true)
public class InMemoryRuntimeAuditStore implements RuntimeAuditStore {

    /**
     * 审计记录序列。
     */
    private final AtomicLong sequence = new AtomicLong();

    /**
     * 审计记录列表。
     */
    private final List<RuntimeAuditRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void saveBatch(List<RuntimeAuditRecord> records) {
        for (RuntimeAuditRecord record : records) {
            // 内存实现用自增序列模拟数据库主键
            record.setId(sequence.incrementAndGet());
            this.records.add(record);
        }
    }

    @Override
    public CursorPage<RuntimeAuditRecord> list(RuntimeAuditQuery query) {
        int effectiveLimit = effectiveLimit(query.getLimit());
        long cursor = cursorValue(query.getCursor());
        List<RuntimeAuditRecord> matched = records.stream()
                .filter(record -> matches(query, record))
                .filter(record -> Objects.requireNonNullElse(record.getId(), RuntimeAuditConstants.EMPTY_CURSOR)
                        > cursor)
                .sorted((left, right) -> Long.compare(left.getId(), right.getId()))
                .limit(effectiveLimit + 1L)
                .toList();
        List<RuntimeAuditRecord> items = trimItems(matched, effectiveLimit);
        CursorPage<RuntimeAuditRecord> page = new CursorPage<>();
        page.setItems(items);
        page.setLimit(effectiveLimit);
        page.setTotal(total(query));
        page.setNextCursor(nextCursor(matched, effectiveLimit));
        return page;
    }

    /**
     * 判断记录是否命中查询条件。
     *
     * @param query 查询条件
     * @param record 审计记录
     * @return 是否命中
     */
    private boolean matches(RuntimeAuditQuery query, RuntimeAuditRecord record) {
        return equalsIfPresent(query.getNamespace(), record.getNamespace())
                && equalsIfPresent(query.getNodeId(), record.getNodeId())
                && equalsIfPresent(query.getProjectName(), record.getProjectName())
                && equalsIfPresent(query.getRouteId(), record.getRouteId())
                && equalsIfPresent(query.getTraceId(), record.getTraceId())
                && equalsIfPresent(query.getOutcome(), record.getOutcome())
                && afterStartedAt(query, record)
                && beforeEndedAt(query, record);
    }

    /**
     * 判断记录是否晚于开始时间。
     *
     * @param query 查询条件
     * @param record 审计记录
     * @return 是否命中
     */
    private boolean afterStartedAt(RuntimeAuditQuery query, RuntimeAuditRecord record) {
        return query.getStartedAt() == null
                || (record.getOccurredAt() != null
                && !record.getOccurredAt().isBefore(query.getStartedAt().toInstant(ZoneOffset.UTC)));
    }

    /**
     * 判断记录是否早于结束时间。
     *
     * @param query 查询条件
     * @param record 审计记录
     * @return 是否命中
     */
    private boolean beforeEndedAt(RuntimeAuditQuery query, RuntimeAuditRecord record) {
        return query.getEndedAt() == null
                || (record.getOccurredAt() != null
                && !record.getOccurredAt().isAfter(query.getEndedAt().toInstant(ZoneOffset.UTC)));
    }

    /**
     * 按条件统计总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    private int total(RuntimeAuditQuery query) {
        return (int) records.stream()
                .filter(record -> matches(query, record))
                .count();
    }

    /**
     * 判断可选条件是否相等。
     *
     * @param expected 期望值
     * @param actual 实际值
     * @return 是否相等
     */
    private boolean equalsIfPresent(String expected, String actual) {
        return !StringUtils.hasText(expected) || Objects.equals(expected, actual);
    }

    /**
     * 裁剪分页结果。
     *
     * @param matched 多取一条的结果
     * @param limit 分页大小
     * @return 当前页结果
     */
    private List<RuntimeAuditRecord> trimItems(List<RuntimeAuditRecord> matched, int limit) {
        if (matched.size() <= limit) {
            return matched;
        }
        return new ArrayList<>(matched.subList(0, limit));
    }

    /**
     * 计算下一页游标。
     *
     * @param matched 多取一条的结果
     * @param limit 分页大小
     * @return 下一页游标
     */
    private String nextCursor(List<RuntimeAuditRecord> matched, int limit) {
        if (matched.size() <= limit) {
            return null;
        }
        return String.valueOf(matched.get(limit - 1).getId());
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
}
