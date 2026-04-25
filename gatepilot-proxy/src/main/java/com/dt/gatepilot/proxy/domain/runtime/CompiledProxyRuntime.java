package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
        String normalizedPath = normalizePath(path);
        if (normalizedPath == null) {
            return null;
        }
        CompiledRoute exactHostRoute = matchByHost(normalizeHost(host), normalizedPath);
        if (exactHostRoute != null) {
            return exactHostRoute;
        }
        return matchByHost(ProxyPathConstants.WILDCARD_HOST, normalizedPath);
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

    private CompiledRoute matchByHost(String host, String path) {
        if (host == null) {
            return null;
        }
        String candidate = path;
        while (candidate != null) {
            CompiledRoute route = routesByHostAndPath.get(new RouteMatchKey(host, candidate));
            if (route != null) {
                return route;
            }
            candidate = parentPath(candidate);
        }
        return null;
    }

    private String parentPath(String path) {
        int lastSlash = path.lastIndexOf(ProxyPathConstants.PATH_SEPARATOR);
        if (lastSlash <= 0) {
            return ProxyPathConstants.ROOT_PATH.equals(path) ? null : ProxyPathConstants.ROOT_PATH;
        }
        return path.substring(0, lastSlash);
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.startsWith(ProxyPathConstants.PATH_SEPARATOR)
                ? path
                : ProxyPathConstants.PATH_SEPARATOR + path;
        int queryIndex = normalized.indexOf(ProxyPathConstants.QUERY_SEPARATOR);
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith(ProxyPathConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        int portIndex = normalized.indexOf(ProxyPathConstants.HOST_PORT_SEPARATOR);
        if (portIndex > 0) {
            return normalized.substring(0, portIndex);
        }
        return normalized;
    }
}
