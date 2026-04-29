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

import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 隔离组资源，用于把项目调度到独立网关副本池
 */
@Data
public class IsolationGroup {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private IsolationGroupSpec spec = new IsolationGroupSpec();

    /**
     * 当前状态
     */
    private IsolationGroupStatus status = new IsolationGroupStatus();

    /**
     * 隔离组期望状态
     */
    @Data
    public static class IsolationGroupSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 隔离组说明
         */
        private String description;

        /**
         * 负责团队
         */
        private String ownerTeam;

        /**
         * 是否独享副本池
         */
        private Boolean dedicated = false;

        /**
         * 节点选择器
         */
        private LabelSelector nodeSelector;

        /**
         * 绑定配置分片
         */
        private List<String> configShards = new ArrayList<>();

        /**
         * 建议最大 RPS
         */
        private Long maxRequestsPerSecond;

        /**
         * 建议最大连接数
         */
        private Long maxActiveConnections;

        /**
         * 隔离组扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 隔离组实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class IsolationGroupStatus extends ResourceStatus {

        /**
         * 节点数量
         */
        private Integer nodeCount;

        /**
         * 就绪节点数量
         */
        private Integer readyNodeCount;

        /**
         * 项目数量
         */
        private Integer projectCount;

        /**
         * 容量状态
         */
        private String capacityState;
    }
}
