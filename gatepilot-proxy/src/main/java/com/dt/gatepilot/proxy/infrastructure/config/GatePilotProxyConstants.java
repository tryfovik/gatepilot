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
package com.dt.gatepilot.proxy.infrastructure.config;

/**
 * GatePilot proxy 配置常量
 */
public final class GatePilotProxyConstants {

    /**
     * proxy 配置前缀
     */
    public static final String CONFIG_PREFIX = "gatepilot.proxy";

    /**
     * proxy 启用开关配置名
     */
    public static final String ENABLED_PROPERTY = "enabled";

    private GatePilotProxyConstants() {
        // proxy 配置常量不允许实例化
    }
}
