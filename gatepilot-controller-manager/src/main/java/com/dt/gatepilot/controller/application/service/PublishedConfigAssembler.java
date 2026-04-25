package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * PublishedConfig 组装器，把声明式资源编译成 agent / proxy 可消费的发布产物。
 */
public class PublishedConfigAssembler {

    /**
     * 组装已发布配置。
     *
     * @param request reconcile 请求
     * @param desiredState 期望状态
     * @return 已发布配置
     */
    public PublishedConfig assemble(ReconcileRequest request, GatewayDesiredState desiredState) {
        PublishedConfig config = new PublishedConfig();
        config.getMetadata().setName(publishedConfigName(request.getVersion(), request.getConfigShard()));
        config.getMetadata().setNamespace(request.getNamespace());
        config.getMetadata().getLabels().put("gatepilot.io/project", request.getProjectName());
        if (request.getConfigShard() != null) {
            config.getMetadata().getLabels().put("gatepilot.io/config-shard", request.getConfigShard());
        }
        PublishedConfig.PublishedConfigSpec spec = config.getSpec();
        spec.setProjectRef(projectRef(request));
        spec.setVersion(request.getVersion());
        spec.setConfigShard(request.getConfigShard());
        spec.setSequence(request.getSequence());
        spec.setFullSnapshot(true);
        spec.setGeneratedAt(Instant.now());
        spec.setRoutes(desiredState.getRoutes().stream().map(this::routeSnapshot).toList());
        spec.setUpstreams(desiredState.getUpstreams().stream().map(this::upstreamSnapshot).toList());
        spec.setPolicies(policySnapshots(desiredState));
        spec.setTargetNodeRefs(desiredState.getTargetNodes().stream().map(this::nodeRef).toList());
        spec.setConfigHash(hash(spec));
        config.getStatus().setDesiredNodeCount(desiredState.getTargetNodes().size());
        config.getStatus().setAppliedNodeCount(0);
        config.getStatus().setFailedNodeCount(0);
        config.getStatus().setApplyState(ConfigApplyState.PENDING);
        return config;
    }

    private ResourceReference projectRef(ReconcileRequest request) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.GATEWAY_PROJECT);
        reference.setNamespace(request.getNamespace());
        reference.setName(request.getProjectName());
        return reference;
    }

    private String publishedConfigName(String version, String configShard) {
        if (configShard == null) {
            return version;
        }
        return (version + "-" + configShard).replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private ResourceReference nodeRef(GatewayNode node) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(ResourceKind.GATEWAY_NODE);
        reference.setNamespace(node.getMetadata().getNamespace());
        reference.setName(node.getMetadata().getName());
        reference.setUid(node.getMetadata().getUid());
        return reference;
    }

    private PublishedConfig.PublishedRoute routeSnapshot(GatewayRoute route) {
        PublishedConfig.PublishedRoute snapshot = new PublishedConfig.PublishedRoute();
        snapshot.setRouteId(route.getMetadata().getUid());
        snapshot.setSourceRef(ref(ResourceKind.GATEWAY_ROUTE, route.getMetadata().getNamespace(),
                route.getMetadata().getName(), route.getMetadata().getUid()));
        snapshot.setProtocols(route.getSpec().getProtocols());
        snapshot.setHosts(route.getSpec().getHosts());
        snapshot.setPath(Optional.ofNullable(route.getSpec().getPath()).map(GatewayRoute.RoutePathMatch::getValue)
                .orElse(null));
        snapshot.setMethods(route.getSpec().getMethods());
        snapshot.setUpstreamName(Optional.ofNullable(route.getSpec().getUpstreamRef()).map(ResourceReference::getName)
                .orElse(null));
        snapshot.setPolicyNames(route.getSpec().getPolicyRefs().stream().map(ResourceReference::getName).toList());
        return snapshot;
    }

    private PublishedConfig.PublishedUpstream upstreamSnapshot(Upstream upstream) {
        PublishedConfig.PublishedUpstream snapshot = new PublishedConfig.PublishedUpstream();
        snapshot.setName(upstream.getMetadata().getName());
        snapshot.setSourceRef(ref(ResourceKind.UPSTREAM, upstream.getMetadata().getNamespace(),
                upstream.getMetadata().getName(), upstream.getMetadata().getUid()));
        snapshot.setProtocol(upstream.getSpec().getProtocol());
        snapshot.setLoadBalance(Optional.ofNullable(upstream.getSpec().getLoadBalance()).map(Enum::name).orElse(null));
        snapshot.setEndpoints(upstream.getSpec().getEndpoints().stream().map(this::endpointSnapshot).toList());
        return snapshot;
    }

    private PublishedConfig.PublishedEndpoint endpointSnapshot(Upstream.UpstreamEndpoint endpoint) {
        PublishedConfig.PublishedEndpoint snapshot = new PublishedConfig.PublishedEndpoint();
        snapshot.setHost(endpoint.getHost());
        snapshot.setPort(endpoint.getPort());
        snapshot.setWeight(endpoint.getWeight());
        return snapshot;
    }

    private List<PublishedConfig.PublishedPolicy> policySnapshots(GatewayDesiredState desiredState) {
        return Stream.of(
                        desiredState.getTrafficPolicies().stream().map(this::trafficPolicySnapshot),
                        desiredState.getReleasePolicies().stream().map(this::releasePolicySnapshot),
                        desiredState.getAuthPolicies().stream().map(this::authPolicySnapshot)
                )
                .flatMap(stream -> stream)
                .toList();
    }

    private PublishedConfig.PublishedPolicy trafficPolicySnapshot(TrafficPolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot("TrafficPolicy", ResourceKind.TRAFFIC_POLICY,
                policy.getMetadata().getNamespace(), policy.getMetadata().getName(), policy.getMetadata().getUid());
        snapshot.getConfig().put("targetRefs", policy.getSpec().getTargetRefs());
        snapshot.getConfig().put("targetSelector", policy.getSpec().getTargetSelector());
        snapshot.getConfig().put("timeout", policy.getSpec().getTimeout());
        snapshot.getConfig().put("retry", policy.getSpec().getRetry());
        snapshot.getConfig().put("circuitBreaker", policy.getSpec().getCircuitBreaker());
        snapshot.getConfig().put("rateLimit", policy.getSpec().getRateLimit());
        snapshot.getConfig().put("colorRules", policy.getSpec().getColorRules());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy releasePolicySnapshot(ReleasePolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot("ReleasePolicy", ResourceKind.RELEASE_POLICY,
                policy.getMetadata().getNamespace(), policy.getMetadata().getName(), policy.getMetadata().getUid());
        snapshot.getConfig().put("strategy", policy.getSpec().getStrategy());
        snapshot.getConfig().put("stableUpstreamRef", policy.getSpec().getStableUpstreamRef());
        snapshot.getConfig().put("candidateUpstreamRef", policy.getSpec().getCandidateUpstreamRef());
        snapshot.getConfig().put("trafficSplits", policy.getSpec().getTrafficSplits());
        snapshot.getConfig().put("steps", policy.getSpec().getSteps());
        snapshot.getConfig().put("autoRollback", policy.getSpec().getAutoRollback());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy authPolicySnapshot(AuthPolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot("AuthPolicy", ResourceKind.AUTH_POLICY,
                policy.getMetadata().getNamespace(), policy.getMetadata().getName(), policy.getMetadata().getUid());
        snapshot.getConfig().put("type", policy.getSpec().getType());
        snapshot.getConfig().put("targetRefs", policy.getSpec().getTargetRefs());
        snapshot.getConfig().put("targetSelector", policy.getSpec().getTargetSelector());
        snapshot.getConfig().put("anonymousAllowed", policy.getSpec().getAnonymousAllowed());
        snapshot.getConfig().put("credentialRefs", policy.getSpec().getCredentialRefs());
        snapshot.getConfig().put("jwt", policy.getSpec().getJwt());
        snapshot.getConfig().put("apiKey", policy.getSpec().getApiKey());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy basePolicySnapshot(String type,
                                                              ResourceKind kind,
                                                              String namespace,
                                                              String name,
                                                              String uid) {
        PublishedConfig.PublishedPolicy snapshot = new PublishedConfig.PublishedPolicy();
        snapshot.setName(name);
        snapshot.setType(type);
        snapshot.setSourceRef(ref(kind, namespace, name, uid));
        return snapshot;
    }

    private ResourceReference ref(ResourceKind kind, String namespace, String name, String uid) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(kind);
        reference.setNamespace(namespace);
        reference.setName(name);
        reference.setUid(uid);
        return reference;
    }

    private String hash(PublishedConfig.PublishedConfigSpec spec) {
        String canonical = String.join("|",
                Objects.toString(spec.getProjectRef().getNamespace(), ""),
                Objects.toString(spec.getProjectRef().getName(), ""),
                Objects.toString(spec.getVersion(), ""),
                Objects.toString(spec.getConfigShard(), ""),
                Objects.toString(spec.getSequence(), ""),
                spec.getRoutes().stream().map(PublishedConfig.PublishedRoute::getRouteId).sorted()
                        .reduce("", (left, right) -> left + "," + right),
                spec.getUpstreams().stream().map(PublishedConfig.PublishedUpstream::getName).sorted()
                        .reduce("", (left, right) -> left + "," + right),
                spec.getPolicies().stream().map(PublishedConfig.PublishedPolicy::getName).sorted()
                        .reduce("", (left, right) -> left + "," + right)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
