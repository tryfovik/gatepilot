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
package com.dt.gatepilot.agent.infrastructure.apiserver;

/**
 * agent 访问 apiserver 的协议路径常量。
 */
public final class AgentControlPlaneApiPaths {

    /**
     * agent API 前缀。
     */
    public static final String API_PREFIX = "/api/gatepilot/v1/agents";

    /**
     * 注册路径。
     */
    public static final String REGISTER = "/register";

    /**
     * 心跳路径。
     */
    public static final String HEARTBEAT = "/heartbeat";

    /**
     * 配置拉取路径。
     */
    public static final String CONFIG_PULL = "/configs/pull";

    /**
     * 应用结果上报路径。
     */
    public static final String APPLY_RESULTS = "/apply-results";

    /**
     * 运行审计上报路径。
     */
    public static final String AUDITS = "/audits";

    private AgentControlPlaneApiPaths() {
        // agent 协议路径常量不允许实例化
    }
}
