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
package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.dto.RuntimeAuditReportRequest;
import com.dt.gatepilot.apiserver.application.dto.RuntimeAuditReportResponse;
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
     * @param request 上报请求
     * @return 上报响应
     */
    public RuntimeAuditReportResponse report(RuntimeAuditReportRequest request) {
        List<RuntimeAuditRecord> records = Objects.requireNonNullElse(request.getEvents(),
                        List.<RuntimeAuditReportRequest.RuntimeAuditItem>of())
                .stream()
                .map(item -> record(request, item))
                .toList();
        runtimeAuditStore.saveBatch(records);
        RuntimeAuditReportResponse response = new RuntimeAuditReportResponse();
        response.setAcceptedCount(records.size());
        return response;
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
     * @param request 上报请求
     * @param item 审计事件
     * @return 审计记录
     */
    private RuntimeAuditRecord record(RuntimeAuditReportRequest request,
                                      RuntimeAuditReportRequest.RuntimeAuditItem item) {
        RuntimeAuditRecord record = new RuntimeAuditRecord();
        // 命名空间和节点由 agent 上报外层统一赋值
        record.setNamespace(request.getNamespace());
        record.setNodeId(request.getNodeId());
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
