package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.LinkedHashMap;
import java.util.Map;

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
        route.setHosts(source.getHosts());
        route.setPathPrefix(source.getPath());
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
                index.put(new RouteMatchKey("*", route.getPathPrefix()), route);
                continue;
            }
            for (String host : route.getHosts()) {
                index.put(new RouteMatchKey(host, route.getPathPrefix()), route);
            }
        }
        return index;
    }
}
