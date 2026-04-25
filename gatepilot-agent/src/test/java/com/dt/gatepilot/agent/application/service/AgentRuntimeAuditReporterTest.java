package com.dt.gatepilot.agent.application.service;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditEvent;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * agent 运行审计上报器测试。
 */
class AgentRuntimeAuditReporterTest {

    @Test
    void shouldFlushBufferedAuditsAsBatch() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient();
        AgentRuntimeAuditReporter reporter = newReporter(controlPlaneClient, properties(10, 2));
        reporter.record(event("trace-1"));
        reporter.record(event("trace-2"));

        int flushedCount = reporter.flush();

        assertThat(flushedCount).isEqualTo(2);
        assertThat(reporter.bufferedCount()).isZero();
        assertThat(controlPlaneClient.reportedBatch.getNamespace()).isEqualTo("default");
        assertThat(controlPlaneClient.reportedBatch.getNodeId()).isEqualTo("node-1");
        assertThat(controlPlaneClient.reportedBatch.getEvents())
                .extracting(AgentRuntimeAuditEvent::getTraceId)
                .containsExactly("trace-1", "trace-2");
    }

    @Test
    void shouldDropOldestAuditWhenBufferFull() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient();
        AgentRuntimeAuditReporter reporter = newReporter(controlPlaneClient, properties(2, 10));
        reporter.record(event("trace-1"));
        reporter.record(event("trace-2"));
        reporter.record(event("trace-3"));

        reporter.flush();

        assertThat(controlPlaneClient.reportedBatch.getEvents())
                .extracting(AgentRuntimeAuditEvent::getTraceId)
                .containsExactly("trace-2", "trace-3");
    }

    @Test
    void shouldKeepAuditsWhenFlushFailed() {
        StubControlPlaneClient controlPlaneClient = new StubControlPlaneClient();
        controlPlaneClient.failReport = true;
        AgentRuntimeAuditReporter reporter = newReporter(controlPlaneClient, properties(10, 2));
        reporter.record(event("trace-1"));

        assertThatThrownBy(reporter::flush)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("apiserver unavailable");
        assertThat(reporter.bufferedCount()).isEqualTo(1);
    }

    private AgentRuntimeAuditReporter newReporter(StubControlPlaneClient controlPlaneClient,
                                                  GatePilotAgentProperties properties) {
        AgentNodeProfile profile = new AgentNodeProfile();
        // 测试节点身份只填上报批次需要的字段
        profile.setNamespace("default");
        profile.setNodeId("node-1");
        return new AgentRuntimeAuditReporter(profile, controlPlaneClient, properties);
    }

    private GatePilotAgentProperties properties(int bufferCapacity, int flushBatchSize) {
        GatePilotAgentProperties properties = new GatePilotAgentProperties();
        // 缩小缓冲区，方便测试淘汰和批量上报行为
        properties.getAudit().setBufferCapacity(bufferCapacity);
        properties.getAudit().setFlushBatchSize(flushBatchSize);
        return properties;
    }

    private AgentRuntimeAuditEvent event(String traceId) {
        AgentRuntimeAuditEvent event = new AgentRuntimeAuditEvent();
        // 测试只关心批次顺序和关键诊断字段
        event.setTraceId(traceId);
        event.setRouteId("route-a");
        event.setOutcome("SUCCESS");
        event.setOccurredAt(Instant.now());
        return event;
    }

    private static class StubControlPlaneClient implements AgentControlPlaneClient {

        private AgentRuntimeAuditBatch reportedBatch;

        private boolean failReport;

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
            if (failReport) {
                throw new IllegalStateException("apiserver unavailable");
            }
            this.reportedBatch = batch;
        }
    }
}
