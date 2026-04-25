package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.application.command.ReportRuntimeAuditCommand;
import com.dt.gatepilot.apiserver.application.dto.RuntimeAuditReportResult;
import com.dt.gatepilot.apiserver.application.service.GatePilotRuntimeAuditService;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditConstants;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.getboot.web.api.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 运行审计 API。
 */
@RestController
public class GatePilotRuntimeAuditController {

    /**
     * 运行审计服务。
     */
    private final GatePilotRuntimeAuditService runtimeAuditService;

    /**
     * 创建运行审计控制器。
     *
     * @param runtimeAuditService 运行审计服务
     */
    public GatePilotRuntimeAuditController(GatePilotRuntimeAuditService runtimeAuditService) {
        this.runtimeAuditService = runtimeAuditService;
    }

    /**
     * agent 上报运行审计。
     *
     * @param request 上报请求
     * @return 上报结果
     */
    @PostMapping(GatePilotApiPaths.AGENTS + GatePilotApiPaths.AGENT_AUDITS)
    public Mono<ApiResponse<RuntimeAuditReportResult>> report(@Valid @RequestBody ReportRuntimeAuditCommand request) {
        // agent 只负责上报，查询由 apiserver 统一提供
        return Mono.just(ApiResponse.success(runtimeAuditService.report(request),
                RuntimeAuditConstants.MESSAGE_AUDIT_ACCEPTED));
    }

    /**
     * 查询运行审计。
     *
     * @param namespace 命名空间
     * @param nodeId 节点标识
     * @param routeId 路由标识
     * @param traceId TraceId
     * @param outcome 执行结果
     * @param cursor 游标
     * @param limit 返回条数
     * @return 审计分页结果
     */
    @GetMapping(GatePilotApiPaths.AUDITS)
    public Mono<ApiResponse<CursorPage<RuntimeAuditRecord>>> list(
            @RequestParam(defaultValue = ResourceMetadataConstants.DEFAULT_NAMESPACE) String namespace,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String routeId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = RuntimeAuditConstants.DEFAULT_LIMIT_TEXT) int limit) {
        RuntimeAuditQuery query = new RuntimeAuditQuery();
        // 查询条件保持轻量，复杂诊断后续放专门用例
        query.setNamespace(namespace);
        query.setNodeId(nodeId);
        query.setRouteId(routeId);
        query.setTraceId(traceId);
        query.setOutcome(outcome);
        query.setCursor(cursor);
        query.setLimit(limit);
        return Mono.just(ApiResponse.success(runtimeAuditService.list(query)));
    }
}
