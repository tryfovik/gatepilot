package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.domain.enums.ConfigApplyState;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.RegistryAuthType;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.enums.UpstreamDiscoveryType;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.policy.AuthPolicy;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.platform.RegistryCenter;
import com.dt.gatepilot.domain.resource.platform.RegistryCenterConstants;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import com.dt.gatepilot.domain.resource.route.GatewayRoute;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
        spec.setIsolationGroup(desiredState.getProject().getSpec().getIsolationGroup());
        spec.setSequence(request.getSequence());
        spec.setFullSnapshot(true);
        spec.setGeneratedAt(Instant.now());
        spec.setRoutes(desiredState.getRoutes().stream().map(this::routeSnapshot).toList());
        spec.setUpstreams(desiredState.getUpstreams().stream()
                .map(upstream -> upstreamSnapshot(upstream, desiredState))
                .toList());
        spec.setPolicies(policySnapshots(desiredState));
        spec.setTargetNodeRefs(desiredState.getTargetNodes().stream().map(this::nodeRef).toList());
        spec.setConfigHash(hash(spec));
        config.getStatus().setDesiredNodeCount(desiredState.getTargetNodes().size());
        config.getStatus().setAppliedNodeCount(0);
        config.getStatus().setFailedNodeCount(0);
        config.getStatus().setApplyState(ConfigApplyState.PENDING);
        return config;
    }

    /**
     * 组装回滚发布配置。
     *
     * @param request reconcile 请求
     * @param sourceConfig 回滚目标快照中的已发布配置
     * @return 已发布配置
     */
    public PublishedConfig assembleRollback(ReconcileRequest request, PublishedConfig sourceConfig) {
        PublishedConfig config = new PublishedConfig();
        // 回滚也生成新版本，agent 和 proxy 仍按普通 PublishedConfig 消费
        config.getMetadata().setName(publishedConfigName(request.getVersion(), request.getConfigShard()));
        config.getMetadata().setNamespace(request.getNamespace());
        config.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_PROJECT, request.getProjectName());
        if (request.getConfigShard() != null) {
            config.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_CONFIG_SHARD,
                    request.getConfigShard());
        }
        PublishedConfig.PublishedConfigSpec sourceSpec = sourceConfig.getSpec();
        PublishedConfig.PublishedConfigSpec spec = config.getSpec();
        spec.setProjectRef(Objects.requireNonNullElseGet(copyReference(sourceSpec.getProjectRef()),
                () -> projectRef(request)));
        spec.setVersion(request.getVersion());
        spec.setConfigShard(request.getConfigShard());
        spec.setIsolationGroup(sourceSpec.getIsolationGroup());
        spec.setSequence(request.getSequence());
        spec.setFullSnapshot(true);
        spec.setBaseVersion(request.getTargetVersion());
        spec.setGeneratedAt(Instant.now());
        spec.setMinAgentVersion(sourceSpec.getMinAgentVersion());
        spec.setMinProxyVersion(sourceSpec.getMinProxyVersion());
        spec.setRoutes(copyRoutes(sourceSpec.getRoutes()));
        spec.setUpstreams(copyUpstreams(sourceSpec.getUpstreams()));
        spec.setPolicies(copyPolicies(sourceSpec.getPolicies()));
        spec.setTargetNodeRefs(copyReferences(sourceSpec.getTargetNodeRefs()));
        spec.setTargetNodeSelector(sourceSpec.getTargetNodeSelector());
        spec.setExtensions(new LinkedHashMap<>(sourceSpec.getExtensions()));
        spec.getExtensions().put(PublishedConfigConstants.KEY_ROLLBACK_SOURCE_VERSION, sourceSpec.getVersion());
        spec.getExtensions().put(PublishedConfigConstants.KEY_ROLLBACK_SOURCE_HASH, sourceSpec.getConfigHash());
        spec.setConfigHash(hash(spec));
        config.getStatus().setDesiredNodeCount(spec.getTargetNodeRefs().size());
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
        snapshot.setProjectName(projectName(route.getSpec().getProjectRef()));
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

    private PublishedConfig.PublishedUpstream upstreamSnapshot(Upstream upstream, GatewayDesiredState desiredState) {
        PublishedConfig.PublishedUpstream snapshot = new PublishedConfig.PublishedUpstream();
        // 上游发布快照只保留转发所需字段
        snapshot.setName(upstream.getMetadata().getName());
        snapshot.setSourceRef(ref(ResourceKind.UPSTREAM, upstream.getMetadata().getNamespace(),
                upstream.getMetadata().getName(), upstream.getMetadata().getUid()));
        snapshot.setProtocol(upstream.getSpec().getProtocol());
        snapshot.setLoadBalance(loadBalance(upstream));
        snapshot.setEndpoints(upstream.getSpec().getEndpoints().stream().map(this::endpointSnapshot).toList());
        snapshot.setDiscovery(discoverySnapshot(upstream, desiredState));
        snapshot.setHealthCheck(healthCheckSnapshot(upstream.getSpec().getHealthCheck()));
        return snapshot;
    }

    private PublishedConfig.PublishedDiscovery discoverySnapshot(Upstream upstream,
                                                                 GatewayDesiredState desiredState) {
        Upstream.UpstreamDiscoverySpec discovery = upstream.getSpec().getDiscovery();
        UpstreamDiscoveryType type = discovery == null || discovery.getType() == null
                ? UpstreamDiscoveryType.STATIC
                : discovery.getType();
        PublishedConfig.PublishedDiscovery snapshot = new PublishedConfig.PublishedDiscovery();
        snapshot.setType(type);
        if (type == UpstreamDiscoveryType.STATIC) {
            return snapshot;
        }
        RegistryCenter registryCenter = requireRegistryCenter(upstream, desiredState);
        RegistryCenter.RegistryCenterSpec registrySpec = registryCenter.getSpec();
        snapshot.setRegistryRef(copyReference(discovery.getRegistryRef()));
        snapshot.setRegistryType(registrySpec.getType());
        snapshot.setServerAddr(registrySpec.getServerAddr());
        snapshot.setNamespace(text(discovery.getNamespace(), registrySpec.getNamespace()));
        snapshot.setGroup(text(discovery.getGroup(), text(registrySpec.getGroup(),
                RegistryCenterConstants.DEFAULT_NACOS_GROUP)));
        snapshot.setServiceName(discovery.getServiceName());
        snapshot.setClusters(discovery.getClusters() == null ? List.of() : List.copyOf(discovery.getClusters()));
        snapshot.setMetadataSelector(discovery.getMetadataSelector() == null
                ? Map.of()
                : new LinkedHashMap<>(discovery.getMetadataSelector()));
        snapshot.setAuthType(registrySpec.getAuthType() == null ? RegistryAuthType.NONE : registrySpec.getAuthType());
        snapshot.setUsername(registrySpec.getUsername());
        snapshot.setPassword(registrySpec.getPassword());
        snapshot.setAccessKey(registrySpec.getAccessKey());
        snapshot.setSecretKey(registrySpec.getSecretKey());
        snapshot.setHealthyOnly(!Boolean.FALSE.equals(discovery.getHealthyOnly()));
        snapshot.setEnabledOnly(!Boolean.FALSE.equals(discovery.getEnabledOnly()));
        return snapshot;
    }

    private RegistryCenter requireRegistryCenter(Upstream upstream, GatewayDesiredState desiredState) {
        Upstream.UpstreamDiscoverySpec discovery = upstream.getSpec().getDiscovery();
        ResourceReference registryRef = discovery == null ? null : discovery.getRegistryRef();
        if (registryRef == null || !hasText(registryRef.getName())) {
            throw new IllegalArgumentException(PublishedConfigAssemblerConstants.ERROR_REGISTRY_REF_MISSING_PREFIX
                    + upstream.getMetadata().getName());
        }
        String namespace = text(registryRef.getNamespace(), ResourceMetadataConstants.SYSTEM_NAMESPACE);
        return desiredState.getRegistryCenters().stream()
                .filter(item -> Objects.equals(item.getMetadata().getName(), registryRef.getName()))
                .filter(item -> Objects.equals(item.getMetadata().getNamespace(), namespace))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        PublishedConfigAssemblerConstants.ERROR_REGISTRY_CENTER_MISSING_PREFIX
                                + registryRef.getName()));
    }

    private String loadBalance(Upstream upstream) {
        LoadBalanceStrategy strategy = upstream.getSpec().getLoadBalance();
        if (strategy == null) {
            return null;
        }
        if (!strategy.isSupported()) {
            // controller-manager 再拦一次，防止绕过 dry-run 的发布进入数据面
            throw new IllegalArgumentException(PublishedConfigAssemblerConstants.ERROR_UNSUPPORTED_LOAD_BALANCE_PREFIX
                    + upstream.getMetadata().getName()
                    + PublishedConfigAssemblerConstants.ERROR_DETAIL_SEPARATOR
                    + strategy.name());
        }
        return strategy.name();
    }

    private PublishedConfig.PublishedEndpoint endpointSnapshot(Upstream.UpstreamEndpoint endpoint) {
        PublishedConfig.PublishedEndpoint snapshot = new PublishedConfig.PublishedEndpoint();
        // endpoint 不携带健康状态，健康由 agent 另行上报
        snapshot.setHost(endpoint.getHost());
        snapshot.setPort(endpoint.getPort());
        snapshot.setWeight(endpoint.getWeight());
        snapshot.setLabels(endpoint.getLabels() == null ? Map.of() : new LinkedHashMap<>(endpoint.getLabels()));
        return snapshot;
    }

    private PublishedConfig.PublishedHealthCheck healthCheckSnapshot(Upstream.HealthCheckSpec source) {
        PublishedConfig.PublishedHealthCheck snapshot = new PublishedConfig.PublishedHealthCheck();
        if (source == null) {
            return snapshot;
        }
        // 健康检查配置进入发布产物，proxy 本地执行探测
        snapshot.setEnabled(source.getEnabled());
        snapshot.setPath(source.getPath());
        snapshot.setInterval(source.getInterval());
        snapshot.setTimeout(source.getTimeout());
        snapshot.setHealthyThreshold(source.getHealthyThreshold());
        snapshot.setUnhealthyThreshold(source.getUnhealthyThreshold());
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

    private String projectName(ResourceReference reference) {
        return reference == null ? null : reference.getName();
    }

    private List<PublishedConfig.PublishedRoute> copyRoutes(List<PublishedConfig.PublishedRoute> routes) {
        return routes.stream().map(this::copyRoute).toList();
    }

    private PublishedConfig.PublishedRoute copyRoute(PublishedConfig.PublishedRoute route) {
        PublishedConfig.PublishedRoute copy = new PublishedConfig.PublishedRoute();
        // 回滚快照只复制 proxy 运行态需要的字段
        copy.setRouteId(route.getRouteId());
        copy.setSourceRef(copyReference(route.getSourceRef()));
        copy.setProjectName(route.getProjectName());
        copy.setProtocols(new ArrayList<>(route.getProtocols()));
        copy.setHosts(new ArrayList<>(route.getHosts()));
        copy.setPath(route.getPath());
        copy.setMethods(new ArrayList<>(route.getMethods()));
        copy.setStripPrefix(route.getStripPrefix());
        copy.setRewritePathPrefix(route.getRewritePathPrefix());
        copy.setAddHeaders(new LinkedHashMap<>(route.getAddHeaders()));
        copy.setRemoveHeaders(new ArrayList<>(route.getRemoveHeaders()));
        copy.setUpstreamName(route.getUpstreamName());
        copy.setPolicyNames(new ArrayList<>(route.getPolicyNames()));
        return copy;
    }

    private List<PublishedConfig.PublishedUpstream> copyUpstreams(List<PublishedConfig.PublishedUpstream> upstreams) {
        return upstreams.stream().map(this::copyUpstream).toList();
    }

    private PublishedConfig.PublishedUpstream copyUpstream(PublishedConfig.PublishedUpstream upstream) {
        PublishedConfig.PublishedUpstream copy = new PublishedConfig.PublishedUpstream();
        // endpoint 列表复制一份，避免后续改写污染历史快照
        copy.setName(upstream.getName());
        copy.setSourceRef(copyReference(upstream.getSourceRef()));
        copy.setProtocol(upstream.getProtocol());
        copy.setLoadBalance(upstream.getLoadBalance());
        copy.setEndpoints(upstream.getEndpoints().stream().map(this::copyEndpoint).toList());
        copy.setDiscovery(copyDiscovery(upstream.getDiscovery()));
        copy.setHealthCheck(copyHealthCheck(upstream.getHealthCheck()));
        return copy;
    }

    private PublishedConfig.PublishedDiscovery copyDiscovery(PublishedConfig.PublishedDiscovery discovery) {
        if (discovery == null) {
            return null;
        }
        PublishedConfig.PublishedDiscovery copy = new PublishedConfig.PublishedDiscovery();
        // discovery 包含注册中心连接信息，回滚时必须完整保留
        copy.setType(discovery.getType());
        copy.setRegistryRef(copyReference(discovery.getRegistryRef()));
        copy.setRegistryType(discovery.getRegistryType());
        copy.setServerAddr(discovery.getServerAddr());
        copy.setNamespace(discovery.getNamespace());
        copy.setGroup(discovery.getGroup());
        copy.setServiceName(discovery.getServiceName());
        copy.setClusters(discovery.getClusters() == null ? List.of() : List.copyOf(discovery.getClusters()));
        copy.setMetadataSelector(discovery.getMetadataSelector() == null
                ? Map.of()
                : new LinkedHashMap<>(discovery.getMetadataSelector()));
        copy.setAuthType(discovery.getAuthType());
        copy.setUsername(discovery.getUsername());
        copy.setPassword(discovery.getPassword());
        copy.setAccessKey(discovery.getAccessKey());
        copy.setSecretKey(discovery.getSecretKey());
        copy.setHealthyOnly(discovery.getHealthyOnly());
        copy.setEnabledOnly(discovery.getEnabledOnly());
        return copy;
    }

    private PublishedConfig.PublishedEndpoint copyEndpoint(PublishedConfig.PublishedEndpoint endpoint) {
        PublishedConfig.PublishedEndpoint copy = new PublishedConfig.PublishedEndpoint();
        copy.setHost(endpoint.getHost());
        copy.setPort(endpoint.getPort());
        copy.setWeight(endpoint.getWeight());
        copy.setLabels(endpoint.getLabels() == null ? Map.of() : new LinkedHashMap<>(endpoint.getLabels()));
        return copy;
    }

    private PublishedConfig.PublishedHealthCheck copyHealthCheck(PublishedConfig.PublishedHealthCheck healthCheck) {
        PublishedConfig.PublishedHealthCheck copy = new PublishedConfig.PublishedHealthCheck();
        if (healthCheck == null) {
            return copy;
        }
        // 回滚快照复制时不能共享可变对象
        copy.setEnabled(healthCheck.getEnabled());
        copy.setPath(healthCheck.getPath());
        copy.setInterval(healthCheck.getInterval());
        copy.setTimeout(healthCheck.getTimeout());
        copy.setHealthyThreshold(healthCheck.getHealthyThreshold());
        copy.setUnhealthyThreshold(healthCheck.getUnhealthyThreshold());
        return copy;
    }

    private List<PublishedConfig.PublishedPolicy> copyPolicies(List<PublishedConfig.PublishedPolicy> policies) {
        return policies.stream().map(this::copyPolicy).toList();
    }

    private PublishedConfig.PublishedPolicy copyPolicy(PublishedConfig.PublishedPolicy policy) {
        PublishedConfig.PublishedPolicy copy = new PublishedConfig.PublishedPolicy();
        // 策略配置对象不在回滚流程内修改，外层 map 复制即可隔离新增扩展
        copy.setName(policy.getName());
        copy.setSourceRef(copyReference(policy.getSourceRef()));
        copy.setType(policy.getType());
        copy.setConfig(new LinkedHashMap<>(policy.getConfig()));
        return copy;
    }

    private List<ResourceReference> copyReferences(List<ResourceReference> references) {
        return references.stream().map(this::copyReference).toList();
    }

    private ResourceReference copyReference(ResourceReference reference) {
        if (reference == null) {
            return null;
        }
        ResourceReference copy = new ResourceReference();
        // 资源引用按值复制，避免回滚发布污染历史快照
        copy.setKind(reference.getKind());
        copy.setNamespace(reference.getNamespace());
        copy.setName(reference.getName());
        copy.setUid(reference.getUid());
        return copy;
    }

    private String text(String preferred, String fallback) {
        return hasText(preferred) ? preferred : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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
