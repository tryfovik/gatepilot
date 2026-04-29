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
package com.dt.gatepilot.proxy.interfaces.control;

/**
 * proxy runtime control API 路径常量
 */
public final class ProxyRuntimeApiPaths {

    /**
     * proxy runtime control API 前缀
     */
    public static final String PROXY = "/api/gatepilot/v1/proxy";

    /**
     * 配置应用路径
     */
    public static final String APPLY_CONFIG = "/configs/apply";

    /**
     * 配置应用完成提示
     */
    public static final String MESSAGE_APPLY_CONFIG_FINISHED = "proxy 配置应用完成";

    private ProxyRuntimeApiPaths() {
        // proxy runtime control API 路径常量不允许实例化
    }
}
