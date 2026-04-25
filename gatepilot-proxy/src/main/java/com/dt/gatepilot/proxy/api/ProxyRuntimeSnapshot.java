package com.dt.gatepilot.proxy.api;

import lombok.Data;

/**
 * proxy 当前运行态摘要。
 */
@Data
public class ProxyRuntimeSnapshot {

    /**
     * 当前配置版本。
     */
    private String version;

    /**
     * 当前配置哈希。
     */
    private String configHash;

    /**
     * 当前配置分片。
     */
    private String configShard;

    /**
     * 当前加载路由数量。
     */
    private int routeCount;

    /**
     * 当前加载上游数量。
     */
    private int upstreamCount;

    /**
     * 当前加载策略数量。
     */
    private int policyCount;
}
