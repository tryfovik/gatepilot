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
package com.dt.gatepilot.infrastructure.web;

/**
 * Console Web 装配常量。
 */
public final class ConsoleWebConstants {

    /**
     * 首页资源路径。
     */
    public static final String INDEX_HTML_PATH = "static/index.html";

    /**
     * 根路由。
     */
    public static final String ROOT_ROUTE = "/";

    /**
     * 一级前端路由 fallback。
     */
    public static final String TOP_LEVEL_FALLBACK_ROUTE = "/{path:^(?!api|assets|actuator).*$}";

    /**
     * 多级前端路由 fallback。
     */
    public static final String NESTED_FALLBACK_ROUTE = "/{path:^(?!api|assets|actuator).*$}/**";

    private ConsoleWebConstants() {
        // Console Web 常量不允许实例化
    }
}
