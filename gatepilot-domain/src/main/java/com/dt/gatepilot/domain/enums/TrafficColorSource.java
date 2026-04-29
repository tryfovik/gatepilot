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
 * 流量染色来源。
 */
public enum TrafficColorSource {

    /**
     * 请求头。
     */
    HEADER,

    /**
     * Cookie。
     */
    COOKIE,

    /**
     * 查询参数。
     */
    QUERY,

    /**
     * 客户端 IP。
     */
    IP,

    /**
     * JWT Claim。
     */
    JWT_CLAIM,

    /**
     * 网关运行上下文。
     */
    CONTEXT
}
