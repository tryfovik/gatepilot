package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PublishedConfig 编译器，将发布产物转换成 proxy 热路径可读的运行态索引。
 */
public class PublishedConfigCompiler {

    /**
     * 编译发布配置。
     *
     * @param config 已发布配置
     * @return 编译后的运行态
     */
    public CompiledProxyRuntime compile(PublishedConfig config) {
        // 每次发布编译成全新运行态，后面直接原子替换
        CompiledProxyRuntime runtime = new CompiledProxyRuntime();
        runtime.initMetadata(config);
        String projectName = projectName(config);
        runtime.setRoutes(config.getSpec().getRoutes().stream()
                .map(route -> compileRoute(route, projectName))
                .toList());
        runtime.setUpstreamsByName(compileUpstreams(config));
        runtime.setPoliciesByName(compilePolicies(config));
        compileRoutePolicies(runtime);
        runtime.setRoutesByHostAndPath(compileRouteIndex(runtime.getRoutes()));
        return runtime;
    }

    /**
     * 预编译路由绑定策略。
     *
     * @param runtime 已编译运行态
     */
    private void compileRoutePolicies(CompiledProxyRuntime runtime) {
        RouteAccessEvaluator accessEvaluator = new RouteAccessEvaluator();
        TrafficColorResolver trafficColorResolver = new TrafficColorResolver();
        ReleaseUpstreamResolver releaseUpstreamResolver = new ReleaseUpstreamResolver();
        RateLimitPolicyResolver rateLimitPolicyResolver = new RateLimitPolicyResolver();
        CircuitBreakerPolicyResolver circuitBreakerPolicyResolver = new CircuitBreakerPolicyResolver();
        RetryPolicyResolver retryPolicyResolver = new RetryPolicyResolver();
        for (CompiledRoute route : runtime.getRoutes()) {
            // 策略解析在发布 apply 阶段完成，业务请求只读预编译结果
            route.setAuthPolicies(accessEvaluator.compile(runtime, route));
            route.setTrafficColorPolicy(trafficColorResolver.compile(runtime, route));
            route.setReleaseUpstreamPolicy(releaseUpstreamResolver.compile(runtime, route));
            route.setRateLimitPolicy(rateLimitPolicyResolver.resolve(runtime, route).orElse(null));
            route.setCircuitBreakerPolicy(circuitBreakerPolicyResolver.resolve(runtime, route).orElse(null));
            route.setRetryPolicy(retryPolicyResolver.resolve(runtime, route).orElse(null));
            route.setPoliciesPrecompiled(true);
        }
    }

    private CompiledRoute compileRoute(PublishedConfig.PublishedRoute source, String projectName) {
        // 发布产物允许字段缺省，编译期统一兜底
        CompiledRoute route = new CompiledRoute();
        route.setRouteId(source.getRouteId());
        route.setProjectName(projectName);
        route.setProtocols(emptyIfNull(source.getProtocols()));
        route.setHosts(normalizeHosts(source.getHosts()));
        route.setPathPrefix(normalizePath(source.getPath()));
        route.setMethods(emptyIfNull(source.getMethods()));
        route.setStripPrefix(source.getStripPrefix());
        route.setRewritePathPrefix(normalizeOptionalPathPrefix(source.getRewritePathPrefix()));
        route.setAddHeaders(source.getAddHeaders() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(source.getAddHeaders()));
        route.setRemoveHeaders(emptyIfNull(source.getRemoveHeaders()));
        route.setUpstreamName(source.getUpstreamName());
        route.setPolicyNames(emptyIfNull(source.getPolicyNames()));
        return route;
    }

    private String projectName(PublishedConfig config) {
        ResourceReference projectRef = config.getSpec().getProjectRef();
        return projectRef == null ? null : projectRef.getName();
    }

    private Map<String, CompiledUpstream> compileUpstreams(PublishedConfig config) {
        // 上游按名称建索引，转发时不扫列表
        Map<String, CompiledUpstream> upstreams = new LinkedHashMap<>();
        for (PublishedConfig.PublishedUpstream source : config.getSpec().getUpstreams()) {
            CompiledUpstream upstream = new CompiledUpstream();
            upstream.setName(source.getName());
            upstream.setProtocol(source.getProtocol());
            upstream.setLoadBalance(normalizeLoadBalance(source.getLoadBalance()));
            upstream.setEndpoints(source.getEndpoints().stream().map(this::compileEndpoint).toList());
            upstream.setHealthCheck(compileHealthCheck(source.getHealthCheck()));
            upstreams.put(upstream.getName(), upstream);
        }
        return upstreams;
    }

    private String normalizeLoadBalance(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return ProxyLoadBalanceConstants.DEFAULT_STRATEGY;
        }
        String normalized = strategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (!ProxyLoadBalanceConstants.SUPPORTED_STRATEGIES.contains(normalized)) {
            // proxy apply 兜底拒绝旧产物，避免静默降级成轮询
            throw new IllegalArgumentException(ProxyLoadBalanceConstants.ERROR_UNSUPPORTED_STRATEGY_PREFIX
                    + normalized);
        }
        return normalized;
    }

    private CompiledUpstream.CompiledEndpoint compileEndpoint(PublishedConfig.PublishedEndpoint source) {
        // endpoint 保持轻量映射，负载均衡适配层再转换成组件实例
        CompiledUpstream.CompiledEndpoint endpoint = new CompiledUpstream.CompiledEndpoint();
        endpoint.setHost(source.getHost());
        endpoint.setPort(source.getPort());
        endpoint.setWeight(source.getWeight());
        return endpoint;
    }

    private CompiledUpstream.CompiledHealthCheck compileHealthCheck(
            PublishedConfig.PublishedHealthCheck source) {
        CompiledUpstream.CompiledHealthCheck healthCheck = new CompiledUpstream.CompiledHealthCheck();
        if (source == null) {
            return healthCheck;
        }
        // 健康检查只影响本地实例列表，不影响控制面发布状态
        healthCheck.setEnabled(source.getEnabled());
        healthCheck.setPath(source.getPath());
        healthCheck.setTimeout(source.getTimeout());
        healthCheck.setHealthyThreshold(source.getHealthyThreshold());
        healthCheck.setUnhealthyThreshold(source.getUnhealthyThreshold());
        return healthCheck;
    }

    private Map<String, CompiledPolicy> compilePolicies(PublishedConfig config) {
        // 策略按名称索引，路由只保存策略名
        Map<String, CompiledPolicy> policies = new LinkedHashMap<>();
        for (PublishedConfig.PublishedPolicy source : config.getSpec().getPolicies()) {
            CompiledPolicy policy = new CompiledPolicy();
            policy.setName(source.getName());
            policy.setType(source.getType());
            policy.setConfig(source.getConfig());
            policies.put(policy.getName(), policy);
        }
        return policies;
    }

    private Map<RouteMatchKey, CompiledRoute> compileRouteIndex(Iterable<CompiledRoute> routes) {
        // host 为空的路由归到通配 host
        Map<RouteMatchKey, CompiledRoute> index = new LinkedHashMap<>();
        for (CompiledRoute route : routes) {
            if (route.getHosts().isEmpty()) {
                putRoute(index, new RouteMatchKey(ProxyPathConstants.WILDCARD_HOST, route.getPathPrefix()), route);
                continue;
            }
            for (String host : route.getHosts()) {
                putRoute(index, new RouteMatchKey(host, route.getPathPrefix()), route);
            }
        }
        return index;
    }

    private void putRoute(Map<RouteMatchKey, CompiledRoute> index, RouteMatchKey key, CompiledRoute route) {
        // 同一个 host + path 只能有一个路由
        CompiledRoute previous = index.putIfAbsent(key, route);
        if (previous != null) {
            throw new IllegalArgumentException(ProxyPathConstants.ERROR_DUPLICATE_ROUTE_MATCH_KEY
                    + key.host() + key.pathPrefix());
        }
    }

    private List<String> normalizeHosts(List<String> hosts) {
        // 域名统一小写去重，避免运行态重复索引
        if (hosts == null || hosts.isEmpty()) {
            return List.of();
        }
        Set<String> normalizedHosts = new LinkedHashSet<>();
        for (String host : hosts) {
            String normalized = normalizeHost(host);
            if (normalized != null) {
                normalizedHosts.add(normalized);
            }
        }
        return List.copyOf(normalizedHosts);
    }

    private String normalizeHost(String host) {
        // 路由匹配只关心 host，不关心端口
        if (host == null || host.isBlank()) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        int portIndex = normalized.indexOf(ProxyPathConstants.HOST_PORT_SEPARATOR);
        if (portIndex > 0) {
            normalized = normalized.substring(0, portIndex);
        }
        return normalized;
    }

    private String normalizePath(String path) {
        // 入口 path 是必填项，缺失时直接让发布失败
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(ProxyPathConstants.ERROR_ROUTE_PATH_BLANK);
        }
        String normalized = path.trim();
        normalized = normalized.startsWith(ProxyPathConstants.PATH_SEPARATOR)
                ? normalized
                : ProxyPathConstants.PATH_SEPARATOR + normalized;
        int queryIndex = normalized.indexOf(ProxyPathConstants.QUERY_SEPARATOR);
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith(ProxyPathConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeOptionalPathPrefix(String path) {
        // rewrite 前缀允许不配，配置了就统一成标准路径
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        normalized = normalized.startsWith(ProxyPathConstants.PATH_SEPARATOR)
                ? normalized
                : ProxyPathConstants.PATH_SEPARATOR + normalized;
        while (normalized.length() > 1 && normalized.endsWith(ProxyPathConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        // 运行态尽量不用 null 列表，少做热路径判断
        return values == null ? List.of() : values;
    }
}
