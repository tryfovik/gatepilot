package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
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
import java.util.Map;
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
        // PublishedConfig 是 agent 和 proxy 的唯一发布产物
        config.getMetadata().setName(publishedConfigName(request.getVersion(), request.getConfigShard()));
        config.getMetadata().setNamespace(request.getNamespace());
        config.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_PROJECT, request.getProjectName());
        if (request.getConfigShard() != null) {
            config.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD,
                    request.getConfigShard());
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
        // 发布产物里保留项目引用，方便控制面反查来源
        reference.setKind(ResourceKind.GATEWAY_PROJECT);
        reference.setNamespace(request.getNamespace());
        reference.setName(request.getProjectName());
        return reference;
    }

    private String publishedConfigName(String version, String configShard) {
        if (configShard == null) {
            return version;
        }
        // 资源名需要清理成 Kubernetes 风格的安全字符
        return (version + PublishedConfigAssemblerConstants.NAME_SEPARATOR + configShard)
                .replaceAll(PublishedConfigAssemblerConstants.SAFE_NAME_REGEX,
                        PublishedConfigAssemblerConstants.NAME_SEPARATOR);
    }

    private ResourceReference nodeRef(GatewayNode node) {
        ResourceReference reference = new ResourceReference();
        // targetNodeRefs 只保存节点资源引用，不复制节点状态
        reference.setKind(ResourceKind.GATEWAY_NODE);
        reference.setNamespace(node.getMetadata().getNamespace());
        reference.setName(node.getMetadata().getName());
        reference.setUid(node.getMetadata().getUid());
        return reference;
    }

    private PublishedConfig.PublishedRoute routeSnapshot(GatewayRoute route) {
        // 路由发布快照只保留 proxy 热路径需要的字段
        PublishedConfig.PublishedRoute snapshot = new PublishedConfig.PublishedRoute();
        snapshot.setRouteId(route.getMetadata().getUid());
        snapshot.setSourceRef(ref(ResourceKind.GATEWAY_ROUTE, route.getMetadata().getNamespace(),
                route.getMetadata().getName(), route.getMetadata().getUid()));
        snapshot.setProtocols(route.getSpec().getProtocols());
        snapshot.setHosts(route.getSpec().getHosts());
        snapshot.setPath(Optional.ofNullable(route.getSpec().getPath()).map(GatewayRoute.RoutePathMatch::getValue)
                .orElse(null));
        snapshot.setMethods(route.getSpec().getMethods());
        snapshot.setStripPrefix(Optional.ofNullable(route.getSpec().getPath())
                .map(GatewayRoute.RoutePathMatch::getStripPrefix)
                .orElse(null));
        // rewrite 可能为空，发布产物里统一转成空集合或 null
        GatewayRoute.RewriteRule rewrite = route.getSpec().getRewrite();
        snapshot.setRewritePathPrefix(Optional.ofNullable(rewrite)
                .map(GatewayRoute.RewriteRule::getPathPrefix)
                .orElse(null));
        snapshot.setAddHeaders(rewrite == null ? Map.of() : rewrite.getAddHeaders());
        snapshot.setRemoveHeaders(rewrite == null ? List.of() : rewrite.getRemoveHeaders());
        snapshot.setUpstreamName(Optional.ofNullable(route.getSpec().getUpstreamRef()).map(ResourceReference::getName)
                .orElse(null));
        snapshot.setPolicyNames(route.getSpec().getPolicyRefs().stream().map(ResourceReference::getName).toList());
        return snapshot;
    }

    private PublishedConfig.PublishedUpstream upstreamSnapshot(Upstream upstream) {
        PublishedConfig.PublishedUpstream snapshot = new PublishedConfig.PublishedUpstream();
        // 上游发布快照只保留转发所需字段
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
        // endpoint 不携带健康状态，健康由 agent 另行上报
        snapshot.setHost(endpoint.getHost());
        snapshot.setPort(endpoint.getPort());
        snapshot.setWeight(endpoint.getWeight());
        return snapshot;
    }

    private List<PublishedConfig.PublishedPolicy> policySnapshots(GatewayDesiredState desiredState) {
        // 三类策略合并成 PublishedPolicy 列表，proxy 按 type 识别
        return Stream.of(
                        desiredState.getTrafficPolicies().stream().map(this::trafficPolicySnapshot),
                        desiredState.getReleasePolicies().stream().map(this::releasePolicySnapshot),
                        desiredState.getAuthPolicies().stream().map(this::authPolicySnapshot)
                )
                .flatMap(stream -> stream)
                .toList();
    }

    private PublishedConfig.PublishedPolicy trafficPolicySnapshot(TrafficPolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot(PublishedConfigConstants.POLICY_TYPE_TRAFFIC,
                ResourceKind.TRAFFIC_POLICY, policy.getMetadata().getNamespace(), policy.getMetadata().getName(),
                policy.getMetadata().getUid());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TARGET_REFS, policy.getSpec().getTargetRefs());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TARGET_SELECTOR, policy.getSpec().getTargetSelector());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TIMEOUT, policy.getSpec().getTimeout());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_RETRY, policy.getSpec().getRetry());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_CIRCUIT_BREAKER, policy.getSpec().getCircuitBreaker());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_RATE_LIMIT, policy.getSpec().getRateLimit());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_COLOR_RULES, policy.getSpec().getColorRules());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy releasePolicySnapshot(ReleasePolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot(PublishedConfigConstants.POLICY_TYPE_RELEASE,
                ResourceKind.RELEASE_POLICY, policy.getMetadata().getNamespace(), policy.getMetadata().getName(),
                policy.getMetadata().getUid());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_STRATEGY, policy.getSpec().getStrategy());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_STABLE_UPSTREAM_REF,
                policy.getSpec().getStableUpstreamRef());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_CANDIDATE_UPSTREAM_REF,
                policy.getSpec().getCandidateUpstreamRef());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TRAFFIC_SPLITS, policy.getSpec().getTrafficSplits());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_STEPS, policy.getSpec().getSteps());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_AUTO_ROLLBACK, policy.getSpec().getAutoRollback());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy authPolicySnapshot(AuthPolicy policy) {
        PublishedConfig.PublishedPolicy snapshot = basePolicySnapshot(PublishedConfigConstants.POLICY_TYPE_AUTH,
                ResourceKind.AUTH_POLICY, policy.getMetadata().getNamespace(), policy.getMetadata().getName(),
                policy.getMetadata().getUid());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TYPE, policy.getSpec().getType());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TARGET_REFS, policy.getSpec().getTargetRefs());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_TARGET_SELECTOR, policy.getSpec().getTargetSelector());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_ANONYMOUS_ALLOWED,
                policy.getSpec().getAnonymousAllowed());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_CREDENTIAL_REFS, policy.getSpec().getCredentialRefs());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_JWT, policy.getSpec().getJwt());
        snapshot.getConfig().put(PublishedConfigConstants.KEY_API_KEY, policy.getSpec().getApiKey());
        return snapshot;
    }

    private PublishedConfig.PublishedPolicy basePolicySnapshot(String type,
                                                              ResourceKind kind,
                                                              String namespace,
                                                              String name,
                                                              String uid) {
        PublishedConfig.PublishedPolicy snapshot = new PublishedConfig.PublishedPolicy();
        // policy type 是 proxy 识别策略的稳定契约
        snapshot.setName(name);
        snapshot.setType(type);
        snapshot.setSourceRef(ref(kind, namespace, name, uid));
        return snapshot;
    }

    private ResourceReference ref(ResourceKind kind, String namespace, String name, String uid) {
        ResourceReference reference = new ResourceReference();
        // 只放资源引用，不复制完整资源对象
        reference.setKind(kind);
        reference.setNamespace(namespace);
        reference.setName(name);
        reference.setUid(uid);
        return reference;
    }

    private String hash(PublishedConfig.PublishedConfigSpec spec) {
        // hash 只取影响运行态的关键字段，避免状态字段扰动发布
        String canonical = String.join(PublishedConfigAssemblerConstants.HASH_FIELD_SEPARATOR,
                Objects.toString(spec.getProjectRef().getNamespace(),
                        PublishedConfigAssemblerConstants.EMPTY_HASH_PART),
                Objects.toString(spec.getProjectRef().getName(), PublishedConfigAssemblerConstants.EMPTY_HASH_PART),
                Objects.toString(spec.getVersion(), PublishedConfigAssemblerConstants.EMPTY_HASH_PART),
                Objects.toString(spec.getConfigShard(), PublishedConfigAssemblerConstants.EMPTY_HASH_PART),
                Objects.toString(spec.getSequence(), PublishedConfigAssemblerConstants.EMPTY_HASH_PART),
                spec.getRoutes().stream().map(PublishedConfig.PublishedRoute::getRouteId).sorted()
                        .reduce(PublishedConfigAssemblerConstants.EMPTY_HASH_PART,
                                (left, right) -> left + PublishedConfigAssemblerConstants.HASH_LIST_SEPARATOR + right),
                spec.getUpstreams().stream().map(PublishedConfig.PublishedUpstream::getName).sorted()
                        .reduce(PublishedConfigAssemblerConstants.EMPTY_HASH_PART,
                                (left, right) -> left + PublishedConfigAssemblerConstants.HASH_LIST_SEPARATOR + right),
                spec.getPolicies().stream().map(PublishedConfig.PublishedPolicy::getName).sorted()
                        .reduce(PublishedConfigAssemblerConstants.EMPTY_HASH_PART,
                                (left, right) -> left + PublishedConfigAssemblerConstants.HASH_LIST_SEPARATOR + right)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance(PublishedConfigAssemblerConstants.DIGEST_SHA_256);
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
