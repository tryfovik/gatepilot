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
package com.dt.gatepilot.domain.enums;

/**
 * 路由匹配支持的 HTTP 方法。
 */
public enum HttpMethod {

    /**
     * GET 请求。
     */
    GET,

    /**
     * POST 请求。
     */
    POST,

    /**
     * PUT 请求。
     */
    PUT,

    /**
     * PATCH 请求。
     */
    PATCH,

    /**
     * DELETE 请求。
     */
    DELETE,

    /**
     * HEAD 请求。
     */
    HEAD,

    /**
     * OPTIONS 请求。
     */
    OPTIONS,

    /**
     * 任意 HTTP 方法。
     */
    ANY
}
