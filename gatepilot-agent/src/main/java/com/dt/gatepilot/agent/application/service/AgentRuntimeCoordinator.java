package com.dt.gatepilot.agent.application.service;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.agent.domain.port.PublishedConfigSyncAdapter;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.domain.port.ProxyRuntimeStatusReader;
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

    private final PublishedConfigSyncAdapter configSyncAdapter;

    private final LocalConfigStore localConfigStore;

    private final ProxyApplyClient proxyApplyClient;

    private final ProxyRuntimeStatusReader proxyRuntimeStatusReader;

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
        this(nodeProfile, controlPlaneClient, localConfigStore, proxyApplyClient, null);
    }

    /**
     * 创建 agent 运行编排器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param localConfigStore 本地配置存储
     * @param proxyApplyClient proxy apply 客户端
     * @param proxyRuntimeStatusReader proxy 运行状态读取器
     */
    public AgentRuntimeCoordinator(AgentNodeProfile nodeProfile,
                                   AgentControlPlaneClient controlPlaneClient,
                                   LocalConfigStore localConfigStore,
                                   ProxyApplyClient proxyApplyClient,
                                   ProxyRuntimeStatusReader proxyRuntimeStatusReader) {
        this(nodeProfile, controlPlaneClient, controlPlaneClient::pullConfig, localConfigStore, proxyApplyClient,
                proxyRuntimeStatusReader);
    }

    /**
     * 创建 agent 运行编排器。
     *
     * @param nodeProfile 节点身份
     * @param controlPlaneClient 控制面客户端
     * @param configSyncAdapter 配置同步适配器
     * @param localConfigStore 本地配置存储
     * @param proxyApplyClient proxy apply 客户端
     * @param proxyRuntimeStatusReader proxy 运行状态读取器
     */
    public AgentRuntimeCoordinator(AgentNodeProfile nodeProfile,
                                   AgentControlPlaneClient controlPlaneClient,
                                   PublishedConfigSyncAdapter configSyncAdapter,
                                   LocalConfigStore localConfigStore,
                                   ProxyApplyClient proxyApplyClient,
                                   ProxyRuntimeStatusReader proxyRuntimeStatusReader) {
        this.nodeProfile = nodeProfile;
        this.controlPlaneClient = controlPlaneClient;
        this.configSyncAdapter = configSyncAdapter;
        this.localConfigStore = localConfigStore;
        this.proxyApplyClient = proxyApplyClient;
        this.proxyRuntimeStatusReader = proxyRuntimeStatusReader;
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
        if (proxyRuntimeStatusReader != null) {
            // 心跳补齐本机 proxy 状态，agent 仍不参与发布决策
            proxyRuntimeStatusReader.fill(snapshot);
        }
        controlPlaneClient.heartbeat(snapshot);
    }

    /**
     * 拉取并应用配置。
     *
     * @return 应用结果；没有新配置时为空
     */
    public Optional<AgentApplyResult> pullAndApply() {
        Optional<PublishedConfig> latestConfig = configSyncAdapter.sync(buildCursor());
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
            // proxy 异常要收敛成可上报的节点结果
            result = failedResult(config, AgentApplyConstants.REASON_PROXY_APPLY_FAILED,
                    exception.getMessage(), startedAt);
        }
        if (result == null) {
            // 空结果按失败处理，避免控制面误判发布成功
            result = failedResult(config, AgentApplyConstants.REASON_PROXY_APPLY_EMPTY_RESULT,
                    AgentApplyConstants.MESSAGE_PROXY_APPLY_EMPTY_RESULT, startedAt);
        }
        // 上报字段统一以当前节点和当前配置为准
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
            result.setReason(AgentApplyConstants.REASON_MISSING_APPLY_STATE);
            result.setMessage(AgentApplyConstants.MESSAGE_MISSING_APPLY_STATE);
        }
        if (result.getState() == ConfigApplyState.APPLIED) {
            // 只有 proxy 真正接住配置后才晋升 last-good
            localConfigStore.promoteLastGood(config);
        }
        return result;
    }

    private AgentApplyResult failedResult(PublishedConfig config,
                                          String reason,
                                          String message,
                                          Instant startedAt) {
        AgentApplyResult result = new AgentApplyResult();
        // 失败结果保留版本和 hash，方便控制面排障
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
        // cursor 只描述本节点当前消费进度
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
