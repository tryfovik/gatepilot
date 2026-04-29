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
package com.dt.gatepilot.proxy.infrastructure.governance;

/**
 * proxy Sentinel 治理常量。
 */
public final class ProxySentinelConstants {

    /**
     * Sentinel 自定义 API 名称前缀。
     */
    public static final String API_NAME_PREFIX = "gatepilot-api-";

    /**
     * getboot 治理配置前缀。
     */
    public static final String GETBOOT_GOVERNANCE_PREFIX = "getboot.governance";

    /**
     * getboot 治理开关。
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * getboot Sentinel 开关。
     */
    public static final String SENTINEL_ENABLED_PROPERTY = "sentinel.enabled";

    /**
     * 开启值。
     */
    public static final String ENABLED_VALUE = "true";

    /**
     * Sentinel 默认统计周期。
     */
    public static final long DEFAULT_INTERVAL_SECONDS = 1L;

    /**
     * Sentinel 默认突发容量。
     */
    public static final int DEFAULT_BURST = 0;

    /**
     * Sentinel 默认排队等待时间。
     */
    public static final int DEFAULT_MAX_QUEUEING_TIMEOUT_MILLIS = 0;

    private ProxySentinelConstants() {
        // proxy Sentinel 常量不允许实例化
    }
}
