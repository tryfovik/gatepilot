package com.dt.gatepilot.domain.resource.config;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配置版本快照，用于 diff、回滚和发布排障。
 */
@Data
public class GatewayConfigSnapshot {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 快照内容。
     */
    private GatewayConfigSnapshotSpec spec = new GatewayConfigSnapshotSpec();

    /**
     * 快照状态。
     */
    private GatewayConfigSnapshotStatus status = new GatewayConfigSnapshotStatus();

    /**
     * 快照内容。
     */
    @Data
    public static class GatewayConfigSnapshotSpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 对应 PublishedConfig。
         */
        private ResourceReference publishedConfigRef;

        /**
         * 来源发布请求标识。
         */
        private String releaseId;

        /**
         * 快照版本。
         */
        private String version;

        /**
         * 配置内容哈希。
         */
        private String configHash;

        /**
         * 配置分片键。
         */
        private String configShard;

        /**
         * 同一分片内序号。
         */
        private Long sequence;

        /**
         * 快照采集时间。
         */
        private Instant capturedAt;

        /**
         * 快照创建人或触发方。
         */
        private String capturedBy;

        /**
         * 快照说明。
         */
        private String description;

        /**
         * 完整 PublishedConfig 快照。
         */
        private PublishedConfig publishedConfig;
    }

    /**
     * 快照状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GatewayConfigSnapshotStatus extends ResourceStatus {

        /**
         * 是否允许回滚到该版本。
         */
        private Boolean rollbackAllowed = true;

        /**
         * 最近一次回滚时间。
         */
        private Instant lastRollbackAt;

        /**
         * 最近一次回滚发布请求标识。
         */
        private String lastRollbackReleaseId;
    }
}
