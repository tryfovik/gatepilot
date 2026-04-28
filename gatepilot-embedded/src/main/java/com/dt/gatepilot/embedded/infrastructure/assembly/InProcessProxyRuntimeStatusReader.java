package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.domain.port.ProxyRuntimeStatusReader;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.NodePhase;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;

/**
 * 单体合包内的 proxy 运行状态读取器。
 */
public class InProcessProxyRuntimeStatusReader implements ProxyRuntimeStatusReader {

    /**
     * proxy 运行态。
     */
    private final ProxyRuntimeState runtimeState;

    /**
     * 上游端点健康状态表。
     */
    private final UpstreamEndpointHealthRegistry healthRegistry;

    /**
     * 上游服务发现注册表。
     */
    private final UpstreamDiscoveryRegistry upstreamDiscoveryRegistry;

    /**
     * 创建进程内 proxy 运行状态读取器。
     *
     * @param runtimeState proxy 运行态
     * @param healthRegistry 上游端点健康状态表
     * @param upstreamDiscoveryRegistry 上游服务发现注册表
     */
    public InProcessProxyRuntimeStatusReader(ProxyRuntimeState runtimeState,
                                             UpstreamEndpointHealthRegistry healthRegistry,
                                             UpstreamDiscoveryRegistry upstreamDiscoveryRegistry) {
        this.runtimeState = runtimeState;
        this.healthRegistry = healthRegistry;
        this.upstreamDiscoveryRegistry = upstreamDiscoveryRegistry;
    }

    /**
     * 填充 proxy 运行状态。
     *
     * @param snapshot 心跳快照
     */
    @Override
    public void fill(AgentHeartbeatSnapshot snapshot) {
        GatewayNodeStatus.NodeHealth health = snapshot.getHealth();
        health.setAgentHealthy(true);
        health.setControlPlaneConnected(true);
        runtimeState.current().ifPresentOrElse(runtime -> fillRuntime(snapshot, runtime),
                () -> fillEmptyRuntime(snapshot));
    }

    private void fillRuntime(AgentHeartbeatSnapshot snapshot, CompiledProxyRuntime runtime) {
        GatewayNodeStatus.NodeHealth health = snapshot.getHealth();
        // 运行态存在即代表 proxy 已接住配置
        snapshot.setNodePhase(NodePhase.READY);
        snapshot.setApplyState(ConfigApplyState.APPLIED);
        health.setProxyHealthy(true);
        snapshot.setCurrentConfigVersion(runtime.getVersion());
        snapshot.setLoadedRouteCount(runtime.getRoutes().size());
        snapshot.setLoadedUpstreamCount(runtime.getUpstreamsByName().size());
        snapshot.setLoadedPolicyCount(runtime.getPoliciesByName().size());
        snapshot.setUpstreamHealth(healthRegistry.snapshot(runtime, upstreamDiscoveryRegistry));
    }

    private void fillEmptyRuntime(AgentHeartbeatSnapshot snapshot) {
        GatewayNodeStatus.NodeHealth health = snapshot.getHealth();
        // proxy 尚未加载配置时节点不能接业务流量
        snapshot.setNodePhase(NodePhase.NOT_READY);
        health.setProxyHealthy(false);
    }
}
