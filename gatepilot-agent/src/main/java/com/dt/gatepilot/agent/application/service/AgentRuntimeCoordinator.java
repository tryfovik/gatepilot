package com.dt.gatepilot.agent.application.service;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * agent 运行编排器，负责注册、心跳、配置拉取、staged、last-good 和 proxy apply。
 */
public class AgentRuntimeCoordinator {

    private final AgentNodeProfile nodeProfile;

    private final AgentControlPlaneClient controlPlaneClient;

    private final LocalConfigStore localConfigStore;

    private final ProxyApplyClient proxyApplyClient;

    /**
     * 创建 agent 运行编排器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param localConfigStore 本地配置存储
     * @param proxyApplyClient proxy apply 客户端
     */
    public AgentRuntimeCoordinator(AgentNodeProfile nodeProfile,
                                   AgentControlPlaneClient controlPlaneClient,
                                   LocalConfigStore localConfigStore,
                                   ProxyApplyClient proxyApplyClient) {
        this.nodeProfile = nodeProfile;
        this.controlPlaneClient = controlPlaneClient;
        this.localConfigStore = localConfigStore;
        this.proxyApplyClient = proxyApplyClient;
    }

    /**
     * 注册当前节点。
     */
    public void register() {
        controlPlaneClient.register(nodeProfile);
    }

    /**
     * 发送心跳。
     *
     * @param snapshot 心跳快照
     */
    public void heartbeat(AgentHeartbeatSnapshot snapshot) {
        snapshot.setNamespace(nodeProfile.getNamespace());
        snapshot.setNodeId(nodeProfile.getNodeId());
        localConfigStore.loadLastGood()
                .map(config -> config.getSpec().getVersion())
                .ifPresent(snapshot::setLastGoodConfigVersion);
        controlPlaneClient.heartbeat(snapshot);
    }

    /**
     * 拉取并应用配置。
     *
     * @return 应用结果；没有新配置时为空
     */
    public Optional<AgentApplyResult> pullAndApply() {
        Optional<PublishedConfig> latestConfig = controlPlaneClient.pullConfig(buildCursor());
        if (latestConfig.isEmpty()) {
            return Optional.empty();
        }
        AgentApplyResult result = apply(latestConfig.get());
        controlPlaneClient.reportApplyResult(result);
        return Optional.of(result);
    }

    /**
     * 使用 last-good 配置启动 proxy。
     *
     * @return 应用结果；没有 last-good 时为空
     */
    public Optional<AgentApplyResult> startWithLastGood() {
        return localConfigStore.loadLastGood().map(this::apply);
    }

    private AgentApplyResult apply(PublishedConfig config) {
        Instant startedAt = Instant.now();
        localConfigStore.saveStaged(config);
        AgentApplyResult result;
        try {
            result = proxyApplyClient.apply(config);
        } catch (RuntimeException exception) {
            result = failedResult(config, "ProxyApplyFailed", exception.getMessage(), startedAt);
        }
        if (result == null) {
            result = failedResult(config, "ProxyApplyEmptyResult", "proxy apply 未返回结果", startedAt);
        }
        result.setNamespace(nodeProfile.getNamespace());
        result.setNodeId(nodeProfile.getNodeId());
        result.setVersion(config.getSpec().getVersion());
        result.setConfigHash(config.getSpec().getConfigHash());
        if (result.getStartedAt() == null) {
            result.setStartedAt(startedAt);
        }
        if (result.getFinishedAt() == null) {
            result.setFinishedAt(Instant.now());
        }
        if (result.getState() == null) {
            result.setState(ConfigApplyState.FAILED);
            result.setReason("MissingApplyState");
            result.setMessage("proxy apply 结果缺少状态");
        }
        if (result.getState() == ConfigApplyState.APPLIED) {
            localConfigStore.promoteLastGood(config);
        }
        return result;
    }

    private AgentApplyResult failedResult(PublishedConfig config,
                                          String reason,
                                          String message,
                                          Instant startedAt) {
        AgentApplyResult result = new AgentApplyResult();
        result.setVersion(config.getSpec().getVersion());
        result.setConfigHash(config.getSpec().getConfigHash());
        result.setState(ConfigApplyState.FAILED);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        result.setReason(reason);
        result.setMessage(message);
        return result;
    }

    private AgentConfigCursor buildCursor() {
        AgentConfigCursor cursor = new AgentConfigCursor();
        cursor.setNamespace(nodeProfile.getNamespace());
        cursor.setNodeId(nodeProfile.getNodeId());
        cursor.setZone(nodeProfile.getZone());
        cursor.setIsolationGroup(nodeProfile.getIsolationGroup());
        cursor.setConfigShards(nodeProfile.getConfigShards());
        localConfigStore.loadLastGood().ifPresent(config -> {
            cursor.setCurrentVersion(config.getSpec().getVersion());
            cursor.setCurrentSequence(Objects.requireNonNullElse(config.getSpec().getSequence(), 0L));
        });
        return cursor;
    }
}
