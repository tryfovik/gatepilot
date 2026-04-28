package com.dt.gatepilot.proxy.infrastructure.discovery;

/**
 * Nacos 上游发现常量。
 */
public final class NacosUpstreamDiscoveryConstants {

    /**
     * 订阅 key 分隔符。
     */
    public static final String KEY_SEPARATOR = "|";

    /**
     * 集群列表分隔符。
     */
    public static final String CLUSTER_SEPARATOR = ",";

    /**
     * 元数据筛选分隔符。
     */
    public static final String METADATA_SEPARATOR = ",";

    /**
     * 元数据键值分隔符。
     */
    public static final String METADATA_KEY_VALUE_SEPARATOR = "=";

    /**
     * 空字段。
     */
    public static final String EMPTY_PART = "";

    /**
     * Nacos 集群标签。
     */
    public static final String LABEL_NACOS_CLUSTER = "nacos.cluster";

    /**
     * Nacos 服务名标签。
     */
    public static final String LABEL_NACOS_SERVICE = "nacos.service";

    /**
     * Nacos 实例 ID 标签。
     */
    public static final String LABEL_NACOS_INSTANCE_ID = "nacos.instance-id";

    private NacosUpstreamDiscoveryConstants() {
        // Nacos 发现常量不允许实例化
    }
}
