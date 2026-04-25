package com.dt.gatepilot.apiserver.application.command;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.NodePhase;
import com.dt.gatepilot.domain.resource.node.GatewayNodeStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * agent 心跳请求。
 */
@Data
public class AgentHeartbeatCommand {

    /**
     * 节点命名空间。
     */
    private String namespace = "default";

    /**
     * 节点标识。
     */
    @NotBlank
    private String nodeId;

    /**
     * 节点状态。
     */
    private NodePhase nodePhase;

    /**
     * 当前配置版本。
     */
    private String currentConfigVersion;

    /**
     * 期望配置版本。
     */
    private String desiredConfigVersion;

    /**
     * last-good 配置版本。
     */
    private String lastGoodConfigVersion;

    /**
     * 配置应用状态。
     */
    private ConfigApplyState applyState;

    /**
     * 当前加载路由数量。
     */
    private Integer loadedRouteCount;

    /**
     * 当前加载上游数量。
     */
    private Integer loadedUpstreamCount;

    /**
     * 当前加载策略数量。
     */
    private Integer loadedPolicyCount;

    /**
     * 节点健康摘要。
     */
    private GatewayNodeStatus.NodeHealth health = new GatewayNodeStatus.NodeHealth();

    /**
     * 节点指标摘要。
     */
    private GatewayNodeStatus.MetricsSummary metricsSummary = new GatewayNodeStatus.MetricsSummary();

    /**
     * 上游健康摘要。
     */
    private List<GatewayNodeStatus.UpstreamHealth> upstreamHealth = new ArrayList<>();

    /**
     * 自定义指标摘要。
     */
    private Map<String, String> metrics = new LinkedHashMap<>();
}
