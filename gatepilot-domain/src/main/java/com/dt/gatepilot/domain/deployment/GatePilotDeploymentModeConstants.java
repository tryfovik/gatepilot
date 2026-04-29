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
package com.dt.gatepilot.domain.deployment;

/**
 * GatePilot 部署模式配置常量
 */
public final class GatePilotDeploymentModeConstants {

    /**
     * GatePilot 配置前缀
     */
    public static final String CONFIG_PREFIX = "gatepilot";

    /**
     * 部署模式配置项
     */
    public static final String MODE_PROPERTY = "mode";

    /**
     * 单体合包模式
     */
    public static final String MODE_STANDALONE = "standalone";

    /**
     * 集群分服务模式
     */
    public static final String MODE_CLUSTER = "cluster";

    private GatePilotDeploymentModeConstants() {
        // 部署模式配置常量不允许实例化
    }
}
