package com.dt.gatepilot.proxy.domain.runtime;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;

/**
 * 上游端点选择器。
 */
public class UpstreamEndpointSelector {

    /**
     * 上游轮询游标。
     */
    private final ConcurrentMap<String, AtomicInteger> cursors = new ConcurrentHashMap<>();

    /**
     * 端点健康状态表。
     */
    private final UpstreamEndpointHealthRegistry healthRegistry;

    /**
     * 创建上游端点选择器。
     */
    public UpstreamEndpointSelector() {
        this(new UpstreamEndpointHealthRegistry());
    }

    /**
     * 创建上游端点选择器。
     *
     * @param healthRegistry 端点健康状态表
     */
    public UpstreamEndpointSelector(UpstreamEndpointHealthRegistry healthRegistry) {
        this.healthRegistry = healthRegistry;
    }

    /**
     * 选择一个上游端点。
     *
     * @param upstream 已编译上游
     * @param hashKey 一致性哈希键
     * @return 上游端点
     */
    public CompiledUpstream.CompiledEndpoint select(CompiledUpstream upstream, String hashKey) {
        if (upstream == null || upstream.getEndpoints().isEmpty()) {
            return null;
        }
        List<CompiledUpstream.CompiledEndpoint> endpoints = selectableEndpoints(upstream);
        if (endpoints.size() == 1) {
            return endpoints.get(0);
        }
        return switch (strategy(upstream.getLoadBalance())) {
            case ProxyLoadBalanceConstants.STRATEGY_WEIGHTED_ROUND_ROBIN -> weightedRoundRobin(upstream, endpoints);
            case ProxyLoadBalanceConstants.STRATEGY_RANDOM -> random(endpoints);
            case ProxyLoadBalanceConstants.STRATEGY_CONSISTENT_HASH -> consistentHash(endpoints, hashKey);
            case ProxyLoadBalanceConstants.STRATEGY_LEAST_CONNECTIONS -> roundRobin(upstream, endpoints);
            default -> roundRobin(upstream, endpoints);
        };
    }

    private CompiledUpstream.CompiledEndpoint roundRobin(CompiledUpstream upstream,
                                                        List<CompiledUpstream.CompiledEndpoint> endpoints) {
        // 每个上游独立维护游标，扩副本时各节点本地均衡
        int cursor = nextCursor(upstream.getName());
        return endpoints.get(Math.floorMod(cursor, endpoints.size()));
    }

    private CompiledUpstream.CompiledEndpoint weightedRoundRobin(CompiledUpstream upstream,
                                                                 List<CompiledUpstream.CompiledEndpoint> endpoints) {
        int totalWeight = endpoints.stream().mapToInt(this::weight).sum();
        int bucket = Math.floorMod(nextCursor(upstream.getName()), totalWeight);
        int current = 0;
        for (CompiledUpstream.CompiledEndpoint endpoint : endpoints) {
            current += weight(endpoint);
            if (bucket < current) {
                return endpoint;
            }
        }
        return endpoints.get(0);
    }

    private CompiledUpstream.CompiledEndpoint random(List<CompiledUpstream.CompiledEndpoint> endpoints) {
        return endpoints.get(ThreadLocalRandom.current().nextInt(endpoints.size()));
    }

    private CompiledUpstream.CompiledEndpoint consistentHash(List<CompiledUpstream.CompiledEndpoint> endpoints,
                                                            String hashKey) {
        CRC32 crc32 = new CRC32();
        crc32.update(Objects.toString(hashKey, "").getBytes(StandardCharsets.UTF_8));
        return endpoints.get((int) (crc32.getValue() % endpoints.size()));
    }

    private int nextCursor(String upstreamName) {
        return cursors.computeIfAbsent(upstreamName, ignored -> new AtomicInteger()).getAndIncrement();
    }

    private int weight(CompiledUpstream.CompiledEndpoint endpoint) {
        Integer weight = endpoint.getWeight();
        return weight == null || weight <= 0 ? ProxyLoadBalanceConstants.MIN_ENDPOINT_WEIGHT : weight;
    }

    private String strategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return ProxyLoadBalanceConstants.DEFAULT_STRATEGY;
        }
        return strategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private List<CompiledUpstream.CompiledEndpoint> selectableEndpoints(CompiledUpstream upstream) {
        List<CompiledUpstream.CompiledEndpoint> endpoints = upstream.getEndpoints().stream()
                .filter(endpoint -> healthRegistry.selectable(upstream.getName(), endpoint))
                .toList();
        // 全部不健康时保持失败开放，让真实请求继续兜底探测
        return endpoints.isEmpty() ? upstream.getEndpoints() : endpoints;
    }
}
