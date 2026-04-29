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
package com.dt.gatepilot.apiserver.infrastructure.config;

/**
 * apiserver 配置常量。
 */
public final class GatePilotApiserverConstants {

    /**
     * apiserver 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.apiserver";

    /**
     * apiserver 启用开关配置名。
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * 资源存储配置前缀。
     */
    public static final String STORE_CONFIG_PREFIX = CONFIG_PREFIX + ".store";

    /**
     * 存储类型配置名。
     */
    public static final String STORE_TYPE_PROPERTY = "type";

    /**
     * 内存存储类型。
     */
    public static final String STORE_TYPE_MEMORY = "memory";

    /**
     * 数据库存储类型。
     */
    public static final String STORE_TYPE_DATABASE = "database";

    private GatePilotApiserverConstants() {
        // apiserver 配置常量不允许实例化
    }
}
