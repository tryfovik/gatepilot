package com.dt.gatepilot.controller.domain.model;

import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 控制面期望状态快照，controller-manager 用它生成 PublishedConfig。
 */
@Data
public class GatewayDesiredState {

    /**
     * 项目资源。
     */
    private GatewayProject project;

    /**
     * 路由资源列表。
     */
    private List<GatewayRoute> routes = new ArrayList<>();

    /**
     * 上游资源列表。
     */
    private List<Upstream> upstreams = new ArrayList<>();

    /**
     * 流量治理策略列表。
     */
    private List<TrafficPolicy> trafficPolicies = new ArrayList<>();

    /**
     * 发布策略列表。
     */
    private List<ReleasePolicy> releasePolicies = new ArrayList<>();

    /**
     * 认证策略列表。
     */
    private List<AuthPolicy> authPolicies = new ArrayList<>();

    /**
     * 本次发布目标节点。
     */
    private List<GatewayNode> targetNodes = new ArrayList<>();
}
