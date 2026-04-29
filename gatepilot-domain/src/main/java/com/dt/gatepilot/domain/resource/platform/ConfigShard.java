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
package com.dt.gatepilot.domain.resource.platform;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配置分片资源，用于大规模项目按分片生成和下发配置
 */
@Data
public class ConfigShard {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private ConfigShardSpec spec = new ConfigShardSpec();

    /**
     * 当前状态
     */
    private ConfigShardStatus status = new ConfigShardStatus();

    /**
     * 配置分片期望状态
     */
    @Data
    public static class ConfigShardSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 分片说明
         */
        private String description;

        /**
         * 负责团队
         */
        private String ownerTeam;

        /**
         * 绑定隔离组
         */
        private String isolationGroup;

        /**
         * 流量等级
         */
        private String trafficTier;

        /**
         * 单分片建议最大项目数
         */
        private Integer maxProjectCount;

        /**
         * 单分片建议最大路由数
         */
        private Integer maxRouteCount;

        /**
         * 是否允许新项目写入
         */
        private Boolean acceptingProjects = true;

        /**
         * 分片扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 配置分片实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ConfigShardStatus extends ResourceStatus {

        /**
         * 项目数量
         */
        private Integer projectCount;

        /**
         * 路由数量
         */
        private Integer routeCount;

        /**
         * 已绑定节点数量
         */
        private Integer nodeCount;

        /**
         * 最近发布版本
         */
        private String latestPublishedVersion;
    }
}
