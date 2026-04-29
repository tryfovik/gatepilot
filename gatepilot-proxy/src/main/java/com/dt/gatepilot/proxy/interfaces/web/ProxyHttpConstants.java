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
package com.dt.gatepilot.proxy.interfaces.web;

import java.util.Set;

/**
 * proxy HTTP 转发常量。
 */
public final class ProxyHttpConstants {

    /**
     * HTTP 协议。
     */
    public static final String SCHEME_HTTP = "http";

    /**
     * HTTPS 协议。
     */
    public static final String SCHEME_HTTPS = "https";

    /**
     * Spring Cloud LoadBalancer 协议。
     */
    public static final String SCHEME_LOAD_BALANCER = "lb";

    /**
     * 根路径。
     */
    public static final String ROOT_PATH = "/";

    /**
     * 路径分隔符。
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 查询串分隔符。
     */
    public static final String QUERY_SEPARATOR = "?";

    /**
     * 管理 API 基础路径。
     */
    public static final String GATEPILOT_API_BASE = "/api/gatepilot";

    /**
     * 管理 API 路径。
     */
    public static final String GATEPILOT_API_PREFIX = "/api/gatepilot/";

    /**
     * 业务 API 代理前缀。
     */
    public static final String API_PROXY_PREFIX = "/api";

    /**
     * 业务 API 代理路径。
     */
    public static final String API_PROXY_PATH = "/api/**";

    /**
     * 本机内部代理路径。
     */
    public static final String INTERNAL_PROXY_PATH = "/internal/**";

    /**
     * 本机内部代理前缀。
     */
    public static final String INTERNAL_PROXY_PREFIX = "/internal";

    /**
     * 本机地址文本。
     */
    public static final String LOCALHOST = "localhost";

    /**
     * 逐跳请求头。
     */
    public static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    private ProxyHttpConstants() {
        // HTTP 转发常量不允许实例化
    }
}
