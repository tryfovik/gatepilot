package com.dt.platform.gateway.infrastructure.audit;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 保存最近访问审计事件的有界内存仓库。
 */
public class GatewayAccessAuditRepository {

    private final GatewayProperties.AuditProperties auditProperties;

    private final ConcurrentLinkedDeque<GatewayAccessAuditEvent> events = new ConcurrentLinkedDeque<>();

    private final AtomicInteger eventCount = new AtomicInteger();

    /**
     * 创建审计事件仓库。
     *
     * @param auditProperties 审计配置
     */
    public GatewayAccessAuditRepository(GatewayProperties.AuditProperties auditProperties) {
        this.auditProperties = auditProperties;
    }

    /**
     * 保存审计事件。
     *
     * @param event 审计事件
     */
    public void save(GatewayAccessAuditEvent event) {
        if (event == null) {
            return;
        }
        events.addFirst(event);
        int capacity = Math.max(1, auditProperties.getRecentCapacity());
        int currentSize = eventCount.incrementAndGet();
        while (currentSize > capacity) {
            if (events.pollLast() == null) {
                eventCount.set(events.size());
                return;
            }
            currentSize = eventCount.decrementAndGet();
        }
    }

    /**
     * 查询最近审计事件。
     *
     * @param query 查询条件
     * @return 审计事件
     */
    public List<GatewayAccessAuditEvent> search(AuditQuery query) {
        AuditQuery normalizedQuery = query == null ? new AuditQuery(null, null, null, null, null, null, null, 100) : query;
        int limit = normalizedQuery.limit() == null ? 100 : Math.max(1, Math.min(normalizedQuery.limit(), 1000));
        List<GatewayAccessAuditEvent> result = new ArrayList<>();
        for (GatewayAccessAuditEvent event : events) {
            if (matches(event, normalizedQuery)) {
                result.add(event);
                if (result.size() >= limit) {
                    break;
                }
            }
        }
        result.sort(Comparator.comparing(GatewayAccessAuditEvent::timestamp).reversed());
        return List.copyOf(result);
    }

    private boolean matches(GatewayAccessAuditEvent event, AuditQuery query) {
        return matchesText(event.traceId(), query.traceId())
                && matchesText(event.projectKey(), query.projectKey())
                && matchesText(event.routeKey(), query.routeKey())
                && matchesText(event.clientIp(), query.clientIp())
                && matchesText(event.trafficColor(), query.trafficColor())
                && matchesText(event.releaseVariant(), query.releaseVariant())
                && (query.status() == null || query.status().equals(event.status()));
    }

    private boolean matchesText(String actual, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(expected.trim());
    }

    /**
     * 审计查询条件。
     */
    public record AuditQuery(String traceId,
                             String projectKey,
                             String routeKey,
                             String clientIp,
                             Integer status,
                             String trafficColor,
                             String releaseVariant,
                             Integer limit) {
    }
}
