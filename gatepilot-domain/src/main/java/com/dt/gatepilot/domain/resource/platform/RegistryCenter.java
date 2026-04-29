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

import com.dt.gatepilot.domain.enums.RegistryAuthType;
import com.dt.gatepilot.domain.enums.RegistryCenterType;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 注册中心资源，用于统一保存服务发现连接信息
 */
@Data
public class RegistryCenter {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private RegistryCenterSpec spec = new RegistryCenterSpec();

    /**
     * 当前状态
     */
    private RegistryCenterStatus status = new RegistryCenterStatus();

    /**
     * 注册中心期望状态
     */
    @Data
    public static class RegistryCenterSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 注册中心类型
         */
        private RegistryCenterType type = RegistryCenterType.NACOS;

        /**
         * 服务端地址
         */
        private String serverAddr;

        /**
         * 默认命名空间
         */
        private String namespace;

        /**
         * 默认分组
         */
        private String group = RegistryCenterConstants.DEFAULT_NACOS_GROUP;

        /**
         * 认证类型
         */
        private RegistryAuthType authType = RegistryAuthType.NONE;

        /**
         * 用户名
         */
        private String username;

        /**
         * 密码
         */
        private String password;

        /**
         * AccessKey
         */
        private String accessKey;

        /**
         * SecretKey
         */
        private String secretKey;

        /**
         * 是否允许新上游使用
         */
        private Boolean acceptingUpstreams = true;

        /**
         * 注册中心说明
         */
        private String description;

        /**
         * 扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 注册中心实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class RegistryCenterStatus extends ResourceStatus {

        /**
         * 最近探测时间
         */
        private Instant lastCheckedAt;

        /**
         * 最近连接状态
         */
        private Boolean connected;

        /**
         * 最近错误说明
         */
        private String message;
    }
}
