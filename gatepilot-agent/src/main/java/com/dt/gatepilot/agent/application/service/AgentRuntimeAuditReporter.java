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
package com.dt.gatepilot.agent.application.service;

import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditEvent;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * agent 运行审计上报器。
 */
public class AgentRuntimeAuditReporter {

    /**
     * 节点身份。
     */
    private final AgentNodeProfile nodeProfile;

    /**
     * 控制面客户端。
     */
    private final AgentControlPlaneClient controlPlaneClient;

    /**
     * agent 配置。
     */
    private final GatePilotAgentProperties properties;

    /**
     * 审计事件缓冲区。
     */
    private final ArrayBlockingQueue<AgentRuntimeAuditEvent> buffer;

    /**
     * 创建 agent 运行审计上报器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param properties agent 配置
     */
    public AgentRuntimeAuditReporter(AgentNodeProfile nodeProfile,
                                     AgentControlPlaneClient controlPlaneClient,
                                     GatePilotAgentProperties properties) {
        this.nodeProfile = nodeProfile;
        this.controlPlaneClient = controlPlaneClient;
        this.properties = properties;
        this.buffer = new ArrayBlockingQueue<>(bufferCapacity());
    }

    /**
     * 写入运行审计事件。
     *
     * @param event 运行审计事件
     */
    public void record(AgentRuntimeAuditEvent event) {
        if (!properties.getAudit().isEnabled() || event == null) {
            return;
        }
        if (buffer.offer(event)) {
            return;
        }
        // 缓冲满时丢弃最旧事件，优先保留最近现场
        buffer.poll();
        buffer.offer(event);
    }

    /**
     * 批量上报运行审计。
     *
     * @return 上报条数
     */
    public int flush() {
        if (!properties.getAudit().isEnabled() || buffer.isEmpty()) {
            return 0;
        }
        List<AgentRuntimeAuditEvent> events = new ArrayList<>();
        buffer.drainTo(events, flushBatchSize());
        if (events.isEmpty()) {
            return 0;
        }
        AgentRuntimeAuditBatch batch = new AgentRuntimeAuditBatch();
        // namespace 和 nodeId 由 agent 统一写入
        batch.setNamespace(nodeProfile.getNamespace());
        batch.setNodeId(nodeProfile.getNodeId());
        batch.setEvents(events);
        try {
            controlPlaneClient.reportRuntimeAudits(batch);
        } catch (RuntimeException exception) {
            // 上报失败时尽量把本批事件放回缓冲，下一轮继续尝试
            requeue(events);
            throw exception;
        }
        return events.size();
    }

    /**
     * 获取当前缓冲条数。
     *
     * @return 缓冲条数
     */
    public int bufferedCount() {
        return buffer.size();
    }

    /**
     * 获取单批上报条数。
     *
     * @return 单批上报条数
     */
    private int flushBatchSize() {
        return Math.max(AgentRuntimeAuditConstants.MIN_FLUSH_BATCH_SIZE,
                properties.getAudit().getFlushBatchSize());
    }

    /**
     * 获取缓冲容量。
     *
     * @return 缓冲容量
     */
    private int bufferCapacity() {
        return Math.max(AgentRuntimeAuditConstants.MIN_BUFFER_CAPACITY,
                properties.getAudit().getBufferCapacity());
    }

    /**
     * 重新放回未上报事件。
     *
     * @param events 未上报事件
     */
    private void requeue(List<AgentRuntimeAuditEvent> events) {
        for (AgentRuntimeAuditEvent event : events) {
            record(event);
        }
    }
}
