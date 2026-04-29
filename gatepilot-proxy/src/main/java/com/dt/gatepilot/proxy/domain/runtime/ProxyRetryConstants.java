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
import java.util.Set;

/**
 * proxy 重试执行常量。
 */
public final class ProxyRetryConstants {

    /**
     * 重试策略键分隔符。
     */
    public static final String RETRY_KEY_SEPARATOR = ":";

    /**
     * 默认最大尝试次数。
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 1;

    /**
     * 最大允许尝试次数。
     */
    public static final int MAX_ALLOWED_ATTEMPTS = 5;

    /**
     * 默认首次退避时间。
     */
    public static final Duration DEFAULT_FIRST_BACKOFF = Duration.ZERO;

    /**
     * 默认最大退避时间。
     */
    public static final Duration DEFAULT_MAX_BACKOFF = Duration.ofSeconds(1);

    /**
     * 默认可重试状态码。
     */
    public static final List<Integer> DEFAULT_RETRY_STATUSES = List.of(502, 503, 504);

    /**
     * 毫秒时间后缀。
     */
    public static final String DURATION_MILLIS_SUFFIX = "ms";

    /**
     * 秒时间后缀。
     */
    public static final String DURATION_SECONDS_SUFFIX = "s";

    /**
     * 默认允许重试的方法。
     */
    public static final Set<String> DEFAULT_RETRY_METHODS = Set.of("GET", "HEAD", "OPTIONS", "DELETE");

    /**
     * 隐藏工具类构造器。
     */
    private ProxyRetryConstants() {
        // proxy 重试常量不允许实例化
    }
}
