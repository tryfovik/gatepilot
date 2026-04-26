package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.command.ReportRuntimeAuditCommand;
import com.dt.gatepilot.apiserver.application.dto.RuntimeAuditReportResult;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditStore;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * 运行审计应用服务。
 */
@Service
@ConditionalOnGatePilotApiserverEnabled
public class GatePilotRuntimeAuditService {

    /**
     * 运行审计存储。
     */
    private final RuntimeAuditStore runtimeAuditStore;

    /**
     * 创建运行审计应用服务。
     *
     * @param runtimeAuditStore 运行审计存储
     */
    public GatePilotRuntimeAuditService(RuntimeAuditStore runtimeAuditStore) {
        this.runtimeAuditStore = runtimeAuditStore;
    }

    /**
     * 接收 agent 上报的运行审计。
     *
     * @param command 上报命令
     * @return 上报结果
     */
    public RuntimeAuditReportResult report(ReportRuntimeAuditCommand command) {
        List<RuntimeAuditRecord> records = Objects.requireNonNullElse(command.getEvents(),
                        List.<ReportRuntimeAuditCommand.RuntimeAuditItem>of())
                .stream()
                .map(item -> record(command, item))
                .toList();
        runtimeAuditStore.saveBatch(records);
        RuntimeAuditReportResult result = new RuntimeAuditReportResult();
        result.setAcceptedCount(records.size());
        return result;
    }

    /**
     * 查询运行审计。
     *
     * @param query 查询条件
     * @return 审计分页结果
     */
    public CursorPage<RuntimeAuditRecord> list(RuntimeAuditQuery query) {
        return runtimeAuditStore.list(query);
    }

    /**
     * 转换运行审计记录。
     *
     * @param command 上报命令
     * @param item 审计事件
     * @return 审计记录
     */
    private RuntimeAuditRecord record(ReportRuntimeAuditCommand command,
                                      ReportRuntimeAuditCommand.RuntimeAuditItem item) {
        RuntimeAuditRecord record = new RuntimeAuditRecord();
        // 命名空间和节点由 agent 上报外层统一赋值
        record.setNamespace(command.getNamespace());
        record.setNodeId(command.getNodeId());
        record.setTraceId(item.getTraceId());
        record.setClientIp(item.getClientIp());
        record.setMethod(item.getMethod());
        record.setPath(item.getPath());
        record.setHost(item.getHost());
        record.setRouteId(item.getRouteId());
        record.setProjectName(item.getProjectName());
        record.setUpstreamName(item.getUpstreamName());
        record.setUpstreamUri(item.getUpstreamUri());
        record.setStatus(item.getStatus());
        record.setLatencyMillis(item.getLatencyMillis());
        record.setTrafficColor(item.getTrafficColor());
        record.setMethodAllowed(item.getMethodAllowed());
        record.setAuthenticationRequired(item.getAuthenticationRequired());
        record.setFallback(item.getFallback());
        record.setOutcome(item.getOutcome());
        record.setReason(item.getReason());
        record.setError(item.getError());
        record.setOccurredAt(item.getOccurredAt() == null ? Instant.now() : item.getOccurredAt());
        return record;
    }
}
