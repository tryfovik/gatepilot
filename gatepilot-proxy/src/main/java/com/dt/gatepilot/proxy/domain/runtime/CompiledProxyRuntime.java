package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * proxy 预编译运行态，配置切换时整体替换，不在热路径修改。
 */
@Data
public class CompiledProxyRuntime {

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * 路由列表。
     */
    private List<CompiledRoute> routes;

    /**
     * 上游索引。
     */
    private Map<String, CompiledUpstream> upstreamsByName = new LinkedHashMap<>();

    /**
     * 策略索引。
     */
    private Map<String, CompiledPolicy> policiesByName = new LinkedHashMap<>();

    /**
     * 域名 + 路径前缀索引。
     */
    private Map<RouteMatchKey, CompiledRoute> routesByHostAndPath = new LinkedHashMap<>();

    /**
     * 基于请求域名和路径匹配路由。
     *
     * @param host 请求域名
     * @param path 请求路径
     * @return 路由
     */
    public CompiledRoute match(String host, String path) {
        return routes.stream()
                .filter(route -> route.getHosts().isEmpty() || route.getHosts().contains(host))
                .filter(route -> path != null && path.startsWith(route.getPathPrefix()))
                .max(Comparator.comparingInt(route -> route.getPathPrefix().length()))
                .orElse(null);
    }

    /**
     * 创建运行态摘要。
     *
     * @return 运行态摘要
     */
    public com.dt.gatepilot.proxy.application.dto.ProxyRuntimeSnapshot snapshot() {
        com.dt.gatepilot.proxy.application.dto.ProxyRuntimeSnapshot snapshot =
                new com.dt.gatepilot.proxy.application.dto.ProxyRuntimeSnapshot();
        snapshot.setVersion(version);
        snapshot.setConfigHash(configHash);
        snapshot.setConfigShard(configShard);
        snapshot.setRouteCount(routes.size());
        snapshot.setUpstreamCount(upstreamsByName.size());
        snapshot.setPolicyCount(policiesByName.size());
        return snapshot;
    }

    /**
     * 从 PublishedConfig 初始化运行态元信息。
     *
     * @param config 已发布配置
     */
    public void initMetadata(PublishedConfig config) {
        setVersion(config.getSpec().getVersion());
        setConfigHash(config.getSpec().getConfigHash());
        setConfigShard(config.getSpec().getConfigShard());
    }
}
