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
package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditEvent;
import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import java.time.Instant;

/**
 * 单体合包内的 proxy 运行审计采集器。
 */
public class InProcessRuntimeAuditSink implements RuntimeAuditSink {

    private final AgentRuntimeAuditReporter runtimeAuditReporter;

    /**
     * 创建进程内运行审计采集器。
     *
     * @param runtimeAuditReporter agent 运行审计上报器
     */
    public InProcessRuntimeAuditSink(AgentRuntimeAuditReporter runtimeAuditReporter) {
        this.runtimeAuditReporter = runtimeAuditReporter;
    }

    @Override
    public void emit(RuntimeAuditEvent event) {
        if (event == null) {
            return;
        }
        // embedded 只做进程内端口桥接，不保存审计数据
        runtimeAuditReporter.record(toAgentEvent(event));
    }

    /**
     * 转换为 agent 上报事件。
     *
     * @param event proxy 运行审计事件
     * @return agent 上报事件
     */
    private AgentRuntimeAuditEvent toAgentEvent(RuntimeAuditEvent event) {
        AgentRuntimeAuditEvent auditEvent = new AgentRuntimeAuditEvent();
        auditEvent.setTraceId(event.traceId());
        auditEvent.setClientIp(event.clientIp());
        auditEvent.setMethod(event.method());
        auditEvent.setPath(event.path());
        auditEvent.setHost(event.host());
        auditEvent.setRouteId(event.routeId());
        auditEvent.setProjectName(event.projectName());
        auditEvent.setUpstreamName(event.upstreamName());
        auditEvent.setUpstreamUri(event.upstreamUri());
        auditEvent.setStatus(event.status());
        auditEvent.setLatencyMillis(event.latencyMillis());
        auditEvent.setTrafficColor(event.trafficColor());
        auditEvent.setMethodAllowed(event.methodAllowed());
        auditEvent.setAuthenticationRequired(event.authenticationRequired());
        auditEvent.setFallback(event.fallback());
        auditEvent.setOutcome(event.outcome());
        auditEvent.setReason(event.reason());
        auditEvent.setError(event.error());
        auditEvent.setOccurredAt(Instant.now());
        return auditEvent;
    }
}
