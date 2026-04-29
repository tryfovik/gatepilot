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
package com.dt.gatepilot.proxy.infrastructure.metrics;

/**
 * proxy Micrometer 指标常量。
 */
public final class ProxyMetricsConstants {

    /**
     * 路由请求总量指标。
     */
    public static final String ROUTE_REQUESTS_METRIC = "gatepilot.proxy.route.requests";

    /**
     * 路由请求延迟指标。
     */
    public static final String ROUTE_LATENCY_METRIC = "gatepilot.proxy.route.latency";

    /**
     * 路由请求总量说明。
     */
    public static final String ROUTE_REQUESTS_DESCRIPTION = "GatePilot proxy route request count";

    /**
     * 路由请求延迟说明。
     */
    public static final String ROUTE_LATENCY_DESCRIPTION = "GatePilot proxy route request latency";

    /**
     * 路由标识标签。
     */
    public static final String TAG_ROUTE_ID = "route_id";

    /**
     * 状态码标签。
     */
    public static final String TAG_STATUS = "status";

    /**
     * 状态码分组标签。
     */
    public static final String TAG_STATUS_CLASS = "status_class";

    /**
     * 未知标签值。
     */
    public static final String TAG_UNKNOWN = "unknown";

    /**
     * 状态码分组后缀。
     */
    public static final String STATUS_CLASS_SUFFIX = "xx";

    /**
     * 最小 HTTP 状态码。
     */
    public static final int MIN_HTTP_STATUS = 100;

    /**
     * HTTP 状态码分组除数。
     */
    public static final int STATUS_CLASS_DIVISOR = 100;

    private ProxyMetricsConstants() {
        // proxy 指标常量不允许实例化
    }
}
