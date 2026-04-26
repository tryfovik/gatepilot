package com.dt.gatepilot.apiserver.domain.resource;

import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * GatePilot 资源类型注册表。
 */
@Component
@ConditionalOnGatePilotApiserverEnabled
public class GatePilotResourceRegistry {

    private final Map<String, GatePilotResourceType> resourcesByPath = new LinkedHashMap<>();

    /**
     * 初始化资源注册表。
     */
    public GatePilotResourceRegistry() {
        // 注册表是资源 path 到资源类型的唯一入口
        register(GatePilotResourcePaths.PROJECTS, ResourceKind.GATEWAY_PROJECT, GatewayProject.class);
        register(GatePilotResourcePaths.ROUTES, ResourceKind.GATEWAY_ROUTE, GatewayRoute.class);
        register(GatePilotResourcePaths.TRAFFIC_POLICIES, ResourceKind.TRAFFIC_POLICY, TrafficPolicy.class);
        register(GatePilotResourcePaths.RELEASE_POLICIES, ResourceKind.RELEASE_POLICY, ReleasePolicy.class);
        register(GatePilotResourcePaths.AUTH_POLICIES, ResourceKind.AUTH_POLICY, AuthPolicy.class);
        register(GatePilotResourcePaths.UPSTREAMS, ResourceKind.UPSTREAM, Upstream.class);
        register(GatePilotResourcePaths.PUBLISHED_CONFIGS, ResourceKind.PUBLISHED_CONFIG, PublishedConfig.class);
        register(GatePilotResourcePaths.CONFIG_SNAPSHOTS, ResourceKind.CONFIG_SNAPSHOT, GatewayConfigSnapshot.class);
        register(GatePilotResourcePaths.NODES, ResourceKind.GATEWAY_NODE, GatewayNode.class);
        register(GatePilotResourcePaths.EVENTS, ResourceKind.GATEWAY_EVENT, GatewayEvent.class);
    }

    /**
     * 按 URL 资源类型查找注册信息。
     *
     * @param path URL 资源类型
     * @return 注册信息
     */
    public Optional<GatePilotResourceType> findByPath(String path) {
        return Optional.ofNullable(resourcesByPath.get(path));
    }

    private void register(String path, ResourceKind kind, Class<?> javaType) {
        // 使用 LinkedHashMap 保持资源展示顺序稳定
        resourcesByPath.put(path, new GatePilotResourceType(path, kind, javaType));
    }
}
