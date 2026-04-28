package com.dt.gatepilot.apiserver.domain.resource;

import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.config.GatewayConfigSnapshot;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.platform.ConfigShard;
import com.dt.gatepilot.domain.resource.platform.ControlPlaneSetting;
import com.dt.gatepilot.domain.resource.platform.GatewayNamespace;
import com.dt.gatepilot.domain.resource.platform.IngressDomain;
import com.dt.gatepilot.domain.resource.platform.IsolationGroup;
import com.dt.gatepilot.domain.resource.platform.PlatformEnvironment;
import com.dt.gatepilot.domain.resource.platform.PlatformTeam;
import com.dt.gatepilot.domain.resource.platform.RegistryCenter;
import com.dt.gatepilot.domain.resource.platform.TrafficTier;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import org.springframework.stereotype.Component;

/**
 * 资源 metadata 访问工具，避免 apiserver 依赖反射字符串。
 */
@Component
@ConditionalOnGatePilotApiserverEnabled
public class ResourceMetadataSupport {

    /**
     * 读取资源 metadata。
     *
     * @param resource 资源对象
     * @return metadata
     */
    public ResourceMetadata metadataOf(Object resource) {
        if (resource instanceof GatewayNamespace item) {
            return item.getMetadata();
        }
        if (resource instanceof PlatformTeam item) {
            return item.getMetadata();
        }
        if (resource instanceof PlatformEnvironment item) {
            return item.getMetadata();
        }
        if (resource instanceof ConfigShard item) {
            return item.getMetadata();
        }
        if (resource instanceof IsolationGroup item) {
            return item.getMetadata();
        }
        if (resource instanceof ControlPlaneSetting item) {
            return item.getMetadata();
        }
        if (resource instanceof RegistryCenter item) {
            return item.getMetadata();
        }
        if (resource instanceof TrafficTier item) {
            return item.getMetadata();
        }
        if (resource instanceof IngressDomain item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayProject item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayRoute item) {
            return item.getMetadata();
        }
        if (resource instanceof TrafficPolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof ReleasePolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof AuthPolicy item) {
            return item.getMetadata();
        }
        if (resource instanceof Upstream item) {
            return item.getMetadata();
        }
        if (resource instanceof PublishedConfig item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayConfigSnapshot item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayNode item) {
            return item.getMetadata();
        }
        if (resource instanceof GatewayEvent item) {
            return item.getMetadata();
        }
        throw new IllegalArgumentException("unsupported resource type: " + resource.getClass().getName());
    }
}
