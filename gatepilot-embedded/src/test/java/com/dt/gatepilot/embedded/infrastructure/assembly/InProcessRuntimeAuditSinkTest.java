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

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 进程内运行审计采集器测试。
 */
class InProcessRuntimeAuditSinkTest {

    @Test
    void shouldBridgeProxyAuditEventToAgentReporter() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient();
        AgentRuntimeAuditReporter reporter = new AgentRuntimeAuditReporter(profile(), controlPlaneClient,
                new GatePilotAgentProperties());
        InProcessRuntimeAuditSink sink = new InProcessRuntimeAuditSink(reporter);

        sink.emit(new RuntimeAuditSink.RuntimeAuditEvent(
                "trace-1",
                "127.0.0.1",
                "GET",
                "/orders/1",
                "api.test",
                "route-a",
                "project-a",
                "order-service",
                "http://order-service",
                200,
                12,
                "blue",
                true,
                false,
                false,
                "SUCCESS",
                "matched",
                null
        ));
        reporter.flush();

        assertThat(controlPlaneClient.reportedBatch.getEvents()).hasSize(1);
        assertThat(controlPlaneClient.reportedBatch.getEvents().get(0).getTraceId()).isEqualTo("trace-1");
        assertThat(controlPlaneClient.reportedBatch.getEvents().get(0).getProjectName()).isEqualTo("project-a");
        assertThat(controlPlaneClient.reportedBatch.getEvents().get(0).getRouteId()).isEqualTo("route-a");
        assertThat(controlPlaneClient.reportedBatch.getEvents().get(0).getTrafficColor()).isEqualTo("blue");
    }

    private AgentNodeProfile profile() {
        AgentNodeProfile profile = new AgentNodeProfile();
        // 测试节点身份只用于批次外层字段
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        return profile;
    }

    private static class StubControlPlaneClient implements AgentControlPlaneClient {

        private AgentRuntimeAuditBatch reportedBatch;

        @Override
        public void register(AgentNodeProfile profile) {
        }

        @Override
        public void heartbeat(AgentHeartbeatSnapshot heartbeat) {
        }

        @Override
        public Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor) {
            return Optional.empty();
        }

        @Override
        public void reportApplyResult(AgentApplyResult result) {
        }

        @Override
        public void reportRuntimeAudits(AgentRuntimeAuditBatch batch) {
            this.reportedBatch = batch;
        }
    }
}
