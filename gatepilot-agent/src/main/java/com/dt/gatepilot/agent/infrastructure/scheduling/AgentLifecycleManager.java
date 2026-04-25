package com.dt.gatepilot.agent.infrastructure.scheduling;

import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
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

    private final GatePilotAgentProperties properties;

    /**
     * 创建 agent 生命周期调度器。
     *
     * @param coordinator agent 运行编排器
     * @param properties agent 配置
     */
    public AgentLifecycleManager(AgentRuntimeCoordinator coordinator, GatePilotAgentProperties properties) {
        this.coordinator = coordinator;
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

    private void runSafely(Supplier<?> action, String actionName) {
        try {
            // 调度异常只影响本轮，不能让 agent 退出
            action.get();
        } catch (RuntimeException exception) {
            log.warn(AgentLifecycleConstants.LOG_ACTION_FAILED, actionName, exception.getMessage(), exception);
        }
    }
}
