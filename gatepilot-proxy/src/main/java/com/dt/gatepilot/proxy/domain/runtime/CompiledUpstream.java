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
package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.RegistryAuthType;
import com.dt.gatepilot.domain.enums.RegistryCenterType;
import com.dt.gatepilot.domain.enums.UpstreamDiscoveryType;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * proxy 预编译上游。
 */
@Data
public class CompiledUpstream {

    /**
     * 上游名称。
     */
    private String name;

    /**
     * 上游协议。
     */
    private Protocol protocol;

    /**
     * 负载均衡策略。
     */
    private String loadBalance;

    /**
     * 上游端点。
     */
    private List<CompiledEndpoint> endpoints = new ArrayList<>();

    /**
     * 服务发现配置。
     */
    private CompiledDiscovery discovery = new CompiledDiscovery();

    /**
     * 主动健康检查配置。
     */
    private CompiledHealthCheck healthCheck = new CompiledHealthCheck();

    /**
     * 预编译上游端点。
     */
    @Data
    public static class CompiledEndpoint {

        /**
         * 主机名或 IP。
         */
        private String host;

        /**
         * 端口。
         */
        private Integer port;

        /**
         * 权重。
         */
        private Integer weight;

        /**
         * 标签。
         */
        private Map<String, String> labels = new LinkedHashMap<>();
    }

    /**
     * 预编译服务发现配置。
     */
    @Data
    public static class CompiledDiscovery {

        /**
         * 实例发现方式。
         */
        private UpstreamDiscoveryType type = UpstreamDiscoveryType.STATIC;

        /**
         * 注册中心引用。
         */
        private ResourceReference registryRef;

        /**
         * 注册中心类型。
         */
        private RegistryCenterType registryType;

        /**
         * 服务端地址。
         */
        private String serverAddr;

        /**
         * 注册中心命名空间。
         */
        private String namespace;

        /**
         * 注册中心分组。
         */
        private String group;

        /**
         * 注册中心服务名。
         */
        private String serviceName;

        /**
         * Nacos 集群列表。
         */
        private List<String> clusters = new ArrayList<>();

        /**
         * 实例元数据筛选条件。
         */
        private Map<String, String> metadataSelector = new LinkedHashMap<>();

        /**
         * 认证类型。
         */
        private RegistryAuthType authType = RegistryAuthType.NONE;

        /**
         * 用户名。
         */
        private String username;

        /**
         * 密码。
         */
        private String password;

        /**
         * AccessKey。
         */
        private String accessKey;

        /**
         * SecretKey。
         */
        private String secretKey;

        /**
         * 是否只选择健康实例。
         */
        private Boolean healthyOnly = true;

        /**
         * 是否只选择启用实例。
         */
        private Boolean enabledOnly = true;
    }

    /**
     * 预编译健康检查配置。
     */
    @Data
    public static class CompiledHealthCheck {

        /**
         * 是否启用健康检查。
         */
        private Boolean enabled;

        /**
         * 健康检查路径。
         */
        private String path;

        /**
         * 请求超时时间。
         */
        private Duration timeout;

        /**
         * 连续成功阈值。
         */
        private Integer healthyThreshold;

        /**
         * 连续失败阈值。
         */
        private Integer unhealthyThreshold;
    }
}
