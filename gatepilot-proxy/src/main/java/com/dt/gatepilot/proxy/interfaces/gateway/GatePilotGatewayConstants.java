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
package com.dt.gatepilot.proxy.interfaces.gateway;

import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;

/**
 * GatePilot Spring Cloud Gateway 适配常量。
 */
public final class GatePilotGatewayConstants {

    /**
     * GatePilot 数据面兜底路由标识。
     */
    public static final String PROXY_ROUTE_ID = "gatepilot-proxy";

    /**
     * GatePilot 数据面兜底路由顺序。
     */
    public static final int PROXY_ROUTE_ORDER = -100;

    /**
     * 兜底路由占位上游。
     */
    public static final String PROXY_PLACEHOLDER_URI = "http://gatepilot.local";

    /**
     * GatePilot 治理过滤器顺序。
     */
    public static final int GATEWAY_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    /**
     * SCG 重试退避倍率。
     */
    public static final int RETRY_BACKOFF_FACTOR = 2;

    /**
     * SCG 重试退避是否基于上一次结果。
     */
    public static final boolean RETRY_BACKOFF_BASED_ON_PREVIOUS = false;

    /**
     * JSON 序列化失败兜底响应。
     */
    public static final String JSON_SERIALIZE_ERROR_BODY = "{\"code\":500,\"message\":\"网关响应序列化失败\"}";

    private GatePilotGatewayConstants() {
        // SCG 适配常量不允许实例化
    }
}
