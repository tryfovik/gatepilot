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

import com.dt.gatepilot.apiserver.application.command.ReportRuntimeAuditCommand;
import com.dt.gatepilot.apiserver.application.dto.RuntimeAuditReportResult;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryRuntimeAuditStore;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 运行审计应用服务测试。
 */
class GatePilotRuntimeAuditServiceTest {

    @Test
    void shouldReportAndListRuntimeAudits() {
        GatePilotRuntimeAuditService service = new GatePilotRuntimeAuditService(new InMemoryRuntimeAuditStore());
        ReportRuntimeAuditCommand command = new ReportRuntimeAuditCommand();
        Instant baseTime = Instant.parse("2026-04-27T00:00:00Z");
        command.setNamespace("default");
        command.setNodeId("node-1");
        command.getEvents().add(item("trace-1", "project-a", "route-a", "SUCCESS", baseTime.plusSeconds(10)));
        command.getEvents().add(item("trace-2", "project-b", "route-a", "LIMITED", baseTime.plusSeconds(120)));

        RuntimeAuditReportResult result = service.report(command);
        RuntimeAuditQuery query = query("default", "route-a", 10);
        query.setProjectName("project-a");
        query.setEndedAt(toLocalDateTime(baseTime.plusSeconds(30)));
        CursorPage<RuntimeAuditRecord> page = service.list(query);

        assertThat(result.getAcceptedCount()).isEqualTo(2);
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getTraceId()).isEqualTo("trace-1");
        assertThat(page.getItems().get(0).getProjectName()).isEqualTo("project-a");
        assertThat(page.getItems().get(0).getNodeId()).isEqualTo("node-1");
    }

    private ReportRuntimeAuditCommand.RuntimeAuditItem item(String traceId,
                                                           String projectName,
                                                           String routeId,
                                                           String outcome,
                                                           Instant occurredAt) {
        ReportRuntimeAuditCommand.RuntimeAuditItem item = new ReportRuntimeAuditCommand.RuntimeAuditItem();
        // 测试只填查询链路会使用的字段
        item.setTraceId(traceId);
        item.setProjectName(projectName);
        item.setRouteId(routeId);
        item.setOutcome(outcome);
        item.setOccurredAt(occurredAt);
        return item;
    }

    private RuntimeAuditQuery query(String namespace, String routeId, int limit) {
        RuntimeAuditQuery query = new RuntimeAuditQuery();
        // 查询条件保持和 Console 常用过滤一致
        query.setNamespace(namespace);
        query.setRouteId(routeId);
        query.setLimit(limit);
        return query;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
