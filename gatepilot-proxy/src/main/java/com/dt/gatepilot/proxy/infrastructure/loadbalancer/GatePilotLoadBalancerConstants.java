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
package com.dt.gatepilot.proxy.infrastructure.loadbalancer;

/**
 * GatePilot 负载均衡适配常量
 */
public final class GatePilotLoadBalancerConstants {

    /**
     * LoadBalancer 默认配置名称
     */
    public static final String DEFAULT_SPECIFICATION_NAME = "default.gatepilot";

    /**
     * LoadBalancer 配置 Bean 名称
     */
    public static final String CLIENT_SPECIFICATION_BEAN_NAME = "gatePilotLoadBalancerClientSpecification";

    /**
     * 上游服务标识前缀
     */
    public static final String SERVICE_ID_PREFIX = "gp-";

    /**
     * 上游服务标识哈希算法
     */
    public static final String SERVICE_ID_HASH_ALGORITHM = "SHA-256";

    /**
     * 上游服务标识哈希长度
     */
    public static final int SERVICE_ID_HASH_LENGTH = 32;

    /**
     * 端点实例标识分隔符
     */
    public static final String INSTANCE_ID_SEPARATOR = "-";

    /**
     * Spring Cloud LoadBalancer 权重元数据键
     */
    public static final String METADATA_WEIGHT = "weight";

    /**
     * GatePilot 上游名称元数据键
     */
    public static final String METADATA_UPSTREAM_NAME = "gatepilot.upstream";

    /**
     * 默认 HTTP 端口
     */
    public static final int DEFAULT_HTTP_PORT = 80;

    /**
     * 默认 HTTPS 端口
     */
    public static final int DEFAULT_HTTPS_PORT = 443;

    /**
     * 服务标识哈希算法缺失提示
     */
    public static final String ERROR_HASH_ALGORITHM_MISSING = "缺少服务标识哈希算法";

    private GatePilotLoadBalancerConstants() {
        // 负载均衡适配常量不允许实例化
    }
}
