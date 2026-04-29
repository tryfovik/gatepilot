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
 * 入口域名资源，用于项目接入时选择已授权域名
 */
@Data
public class IngressDomain {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private IngressDomainSpec spec = new IngressDomainSpec();

    /**
     * 当前状态
     */
    private IngressDomainStatus status = new IngressDomainStatus();

    /**
     * 入口域名期望状态
     */
    @Data
    public static class IngressDomainSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 域名
         */
        private String host;

        /**
         * 域名说明
         */
        private String description;

        /**
         * 归属团队
         */
        private String ownerTeam;

        /**
         * 默认命名空间
         */
        private String defaultNamespace;

        /**
         * 是否允许新项目使用
         */
        private Boolean acceptingProjects = true;

        /**
         * 域名扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 入口域名实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class IngressDomainStatus extends ResourceStatus {

        /**
         * 路由数量
         */
        private Integer routeCount;
    }
}
