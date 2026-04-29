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
package com.dt.gatepilot.agent.infrastructure.scheduling;

import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditConstants;
import com.dt.gatepilot.agent.application.service.AgentRuntimeAuditReporter;
import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.infrastructure.config.AgentRuntimeConstants;
import com.dt.gatepilot.agent.infrastructure.config.GatePilotAgentProperties;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * agent 生命周期调度器，负责启动和周期性触发运行编排。
 */
@Slf4j
public class AgentLifecycleManager implements ApplicationRunner {

    private final AgentRuntimeCoordinator coordinator;

    private final AgentRuntimeAuditReporter runtimeAuditReporter;

    private final GatePilotAgentProperties properties;

    /**
     * 创建 agent 生命周期调度器。
     *
     * @param coordinator agent 运行编排器
     * @param properties agent 配置
     */
    public AgentLifecycleManager(AgentRuntimeCoordinator coordinator, GatePilotAgentProperties properties) {
        this(coordinator, null, properties);
    }

    /**
     * 创建 agent 生命周期调度器。
     *
     * @param coordinator agent 运行编排器
     * @param runtimeAuditReporter 运行审计上报器
     * @param properties agent 配置
     */
    public AgentLifecycleManager(AgentRuntimeCoordinator coordinator,
                                 AgentRuntimeAuditReporter runtimeAuditReporter,
                                 GatePilotAgentProperties properties) {
        this.coordinator = coordinator;
        this.runtimeAuditReporter = runtimeAuditReporter;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isLifecycleEnabled()) {
            return;
        }
        if (properties.isRegisterOnStartup()) {
            // 注册先行，后续状态上报才能归属到节点
            runSafely(() -> {
                coordinator.register();
                return null;
            }, AgentLifecycleConstants.ACTION_REGISTER);
        }
        if (properties.isStartWithLastGoodOnStartup()) {
            // 控制面不可用时也尽量用上一版配置拉起 proxy
            runSafely(coordinator::startWithLastGood, AgentLifecycleConstants.ACTION_START_LAST_GOOD);
        }
    }

    /**
     * 定时拉取并应用配置。
     */
    @Scheduled(
            fixedDelayString = AgentRuntimeConstants.PULL_INTERVAL_PLACEHOLDER,
            initialDelayString = AgentRuntimeConstants.PULL_INITIAL_DELAY_PLACEHOLDER
    )
    public void pullAndApply() {
        if (!properties.isLifecycleEnabled() || !properties.isPullEnabled()) {
            return;
        }
        // 调度器只发起同步，发布决策仍由控制面完成
        runSafely(coordinator::pullAndApply, AgentLifecycleConstants.ACTION_PULL_AND_APPLY);
    }

    /**
     * 定时上报节点心跳。
     */
    @Scheduled(
            fixedDelayString = AgentRuntimeConstants.HEARTBEAT_INTERVAL_PLACEHOLDER,
            initialDelayString = AgentRuntimeConstants.HEARTBEAT_INITIAL_DELAY_PLACEHOLDER
    )
    public void heartbeat() {
        if (!properties.isLifecycleEnabled() || !properties.isHeartbeatEnabled()) {
            return;
        }
        // 心跳只描述当前节点，不夹带发布推进逻辑
        runSafely(() -> {
            // 运行态指标后续由 proxy 状态端口补齐
            coordinator.heartbeat(new AgentHeartbeatSnapshot());
            return null;
        }, AgentLifecycleConstants.ACTION_HEARTBEAT);
    }

    /**
     * 定时上报运行审计。
     */
    @Scheduled(
            fixedDelayString = AgentRuntimeConstants.AUDIT_FLUSH_INTERVAL_PLACEHOLDER,
            initialDelayString = AgentRuntimeConstants.AUDIT_FLUSH_INITIAL_DELAY_PLACEHOLDER
    )
    public void flushAudits() {
        if (!properties.isLifecycleEnabled() || !properties.getAudit().isEnabled() || runtimeAuditReporter == null) {
            return;
        }
        // 审计上报是旁路链路，失败不能影响配置同步调度
        runSafely(() -> runtimeAuditReporter.flush(), AgentRuntimeAuditConstants.ACTION_FLUSH_AUDITS);
    }

    private void runSafely(Supplier<?> action, String actionName) {
        try {
            // 调度异常只影响本轮，不能让 agent 退出
            action.get();
        } catch (RuntimeException exception) {
            log.warn(AgentLifecycleConstants.LOG_ACTION_FAILED, actionName, exception.getMessage(), exception);
        }
    }
}
