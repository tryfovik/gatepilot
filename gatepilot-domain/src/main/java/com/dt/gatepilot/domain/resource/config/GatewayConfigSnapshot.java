/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
