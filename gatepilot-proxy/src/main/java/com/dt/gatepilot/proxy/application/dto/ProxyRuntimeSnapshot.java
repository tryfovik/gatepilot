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
package com.dt.gatepilot.proxy.application.dto;

import lombok.Data;

/**
 * proxy 当前运行态摘要。
 */
@Data
public class ProxyRuntimeSnapshot {

    /**
     * 当前配置版本。
     */
    private String version;

    /**
     * 当前配置哈希。
     */
    private String configHash;

    /**
     * 当前配置分片。
     */
    private String configShard;

    /**
     * 当前加载路由数量。
     */
    private int routeCount;

    /**
     * 当前加载上游数量。
     */
    private int upstreamCount;

    /**
     * 当前加载策略数量。
     */
    private int policyCount;
}
