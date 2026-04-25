package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
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
        CompiledProxyRuntime runtime = new CompiledProxyRuntime();
        runtime.initMetadata(config);
        runtime.setRoutes(config.getSpec().getRoutes().stream().map(this::compileRoute).toList());
        runtime.setUpstreamsByName(compileUpstreams(config));
        runtime.setPoliciesByName(compilePolicies(config));
        runtime.setRoutesByHostAndPath(compileRouteIndex(runtime.getRoutes()));
        return runtime;
    }

    private CompiledRoute compileRoute(PublishedConfig.PublishedRoute source) {
        CompiledRoute route = new CompiledRoute();
        route.setRouteId(source.getRouteId());
        route.setProtocols(source.getProtocols());
        route.setHosts(normalizeHosts(source.getHosts()));
        route.setPathPrefix(normalizePath(source.getPath()));
        route.setMethods(source.getMethods());
        route.setUpstreamName(source.getUpstreamName());
        route.setPolicyNames(source.getPolicyNames());
        return route;
    }

    private Map<String, CompiledUpstream> compileUpstreams(PublishedConfig config) {
        Map<String, CompiledUpstream> upstreams = new LinkedHashMap<>();
        for (PublishedConfig.PublishedUpstream source : config.getSpec().getUpstreams()) {
            CompiledUpstream upstream = new CompiledUpstream();
            upstream.setName(source.getName());
            upstream.setProtocol(source.getProtocol());
            upstream.setLoadBalance(source.getLoadBalance());
            upstream.setEndpoints(source.getEndpoints().stream().map(this::compileEndpoint).toList());
            upstreams.put(upstream.getName(), upstream);
        }
        return upstreams;
    }

    private CompiledUpstream.CompiledEndpoint compileEndpoint(PublishedConfig.PublishedEndpoint source) {
        CompiledUpstream.CompiledEndpoint endpoint = new CompiledUpstream.CompiledEndpoint();
        endpoint.setHost(source.getHost());
        endpoint.setPort(source.getPort());
        endpoint.setWeight(source.getWeight());
        return endpoint;
    }

    private Map<String, CompiledPolicy> compilePolicies(PublishedConfig config) {
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
        Map<RouteMatchKey, CompiledRoute> index = new LinkedHashMap<>();
        for (CompiledRoute route : routes) {
            if (route.getHosts().isEmpty()) {
                putRoute(index, new RouteMatchKey("*", route.getPathPrefix()), route);
                continue;
            }
            for (String host : route.getHosts()) {
                putRoute(index, new RouteMatchKey(host, route.getPathPrefix()), route);
            }
        }
        return index;
    }

    private void putRoute(Map<RouteMatchKey, CompiledRoute> index, RouteMatchKey key, CompiledRoute route) {
        CompiledRoute previous = index.putIfAbsent(key, route);
        if (previous != null) {
            throw new IllegalArgumentException("duplicate route match key: " + key.host() + key.pathPrefix());
        }
    }

    private List<String> normalizeHosts(List<String> hosts) {
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
        if (host == null || host.isBlank()) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        int portIndex = normalized.indexOf(':');
        if (portIndex > 0) {
            normalized = normalized.substring(0, portIndex);
        }
        return normalized;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("route path must not be blank");
        }
        String normalized = path.trim();
        normalized = normalized.startsWith("/") ? normalized : "/" + normalized;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
