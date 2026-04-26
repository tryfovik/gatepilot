package com.dt.gatepilot.domain.resource.platform;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流量等级资源，用于统一描述项目容量和隔离建议
 */
@Data
public class TrafficTier {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private TrafficTierSpec spec = new TrafficTierSpec();

    /**
     * 当前状态
     */
    private TrafficTierStatus status = new TrafficTierStatus();

    /**
     * 流量等级期望状态
     */
    @Data
    public static class TrafficTierSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 等级说明
         */
        private String description;

        /**
         * 建议最大 RPS
         */
        private Long maxRequestsPerSecond;

        /**
         * 建议最大连接数
         */
        private Long maxActiveConnections;

        /**
         * 推荐配置分片
         */
        private String recommendedConfigShard;

        /**
         * 推荐隔离组
         */
        private String recommendedIsolationGroup;

        /**
         * 是否需要独享副本池
         */
        private Boolean dedicatedSuggested = false;

        /**
         * 流量等级扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 流量等级实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class TrafficTierStatus extends ResourceStatus {

        /**
         * 项目数量
         */
        private Integer projectCount;
    }
}
