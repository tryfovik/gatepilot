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

import java.time.Duration;
import java.util.List;

/**
 * proxy 熔断运行常量。
 */
public final class ProxyCircuitBreakerConstants {

    /**
     * 默认滑动窗口大小。
     */
    public static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;

    /**
     * 默认最小请求数。
     */
    public static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 20;

    /**
     * 默认失败率阈值。
     */
    public static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;

    /**
     * 默认慢调用率阈值。
     */
    public static final int DEFAULT_SLOW_CALL_RATE_THRESHOLD = 100;

    /**
     * 默认半开探测请求数。
     */
    public static final int DEFAULT_HALF_OPEN_CALLS = 10;

    /**
     * 默认打开等待时间。
     */
    public static final Duration DEFAULT_WAIT_DURATION_IN_OPEN_STATE = Duration.ofSeconds(30);

    /**
     * 默认失败状态码。
     */
    public static final List<Integer> DEFAULT_FAILURE_STATUS_CODES = List.of(500, 502, 503, 504);

    /**
     * fallback HTTP 状态。
     */
    public static final int DEFAULT_FALLBACK_STATUS = 503;

    /**
     * fallback 业务码。
     */
    public static final int DEFAULT_FALLBACK_CODE = 503;

    /**
     * fallback 提示。
     */
    public static final String DEFAULT_FALLBACK_MESSAGE = "网关熔断已打开，请稍后重试";

    /**
     * fallback 响应类型。
     */
    public static final String DEFAULT_FALLBACK_CONTENT_TYPE = "application/json";

    /**
     * 毫秒后缀。
     */
    public static final String DURATION_MILLIS_SUFFIX = "ms";

    /**
     * 秒后缀。
     */
    public static final String DURATION_SECONDS_SUFFIX = "s";

    /**
     * 熔断 key 分隔符。
     */
    public static final String CIRCUIT_KEY_SEPARATOR = ":";

    /**
     * 百分比基数。
     */
    public static final double PERCENT_BASE = 100D;

    /**
     * 隐藏工具类构造器。
     */
    private ProxyCircuitBreakerConstants() {
        // proxy 熔断常量不允许实例化
    }
}
