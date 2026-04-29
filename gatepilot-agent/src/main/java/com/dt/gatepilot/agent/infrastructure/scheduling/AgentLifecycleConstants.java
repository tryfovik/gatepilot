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
package com.dt.gatepilot.agent.infrastructure.scheduling;

/**
 * agent 生命周期调度常量。
 */
public final class AgentLifecycleConstants {

    /**
     * 注册节点动作。
     */
    public static final String ACTION_REGISTER = "register";

    /**
     * last-good 启动动作。
     */
    public static final String ACTION_START_LAST_GOOD = "start-last-good";

    /**
     * 拉取并应用配置动作。
     */
    public static final String ACTION_PULL_AND_APPLY = "pull-and-apply";

    /**
     * 心跳动作。
     */
    public static final String ACTION_HEARTBEAT = "heartbeat";

    /**
     * 调度动作失败日志。
     */
    public static final String LOG_ACTION_FAILED = "GatePilot agent action failed, action={}, message={}";

    private AgentLifecycleConstants() {
        // agent 生命周期常量不允许实例化
    }
}
