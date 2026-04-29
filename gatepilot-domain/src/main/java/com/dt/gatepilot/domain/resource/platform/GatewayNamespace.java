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
 * 网关命名空间资源，用于隔离项目、路由、策略和发布
 */
@Data
public class GatewayNamespace {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private GatewayNamespaceSpec spec = new GatewayNamespaceSpec();

    /**
     * 当前状态
     */
    private GatewayNamespaceStatus status = new GatewayNamespaceStatus();

    /**
     * 命名空间期望状态
     */
    @Data
    public static class GatewayNamespaceSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 命名空间说明
         */
        private String description;

        /**
         * 负责团队
         */
        private String ownerTeam;

        /**
         * 默认环境标识
         */
        private String environment;

        /**
         * 默认配置分片
         */
        private String defaultConfigShard;

        /**
         * 默认隔离组
         */
        private String defaultIsolationGroup;

        /**
         * 是否允许业务接入
         */
        private Boolean enabled = true;

        /**
         * 命名空间扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 命名空间实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GatewayNamespaceStatus extends ResourceStatus {

        /**
         * 项目数量
         */
        private Integer projectCount;

        /**
         * 路由数量
         */
        private Integer routeCount;

        /**
         * 节点数量
         */
        private Integer nodeCount;
    }
}
