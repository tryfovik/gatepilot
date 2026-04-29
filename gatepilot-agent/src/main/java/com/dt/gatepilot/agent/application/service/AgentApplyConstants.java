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
package com.dt.gatepilot.agent.application.service;

/**
 * agent 应用配置结果常量。
 */
public final class AgentApplyConstants {

    /**
     * proxy apply 异常原因码。
     */
    public static final String REASON_PROXY_APPLY_FAILED = "ProxyApplyFailed";

    /**
     * proxy 空结果原因码。
     */
    public static final String REASON_PROXY_APPLY_EMPTY_RESULT = "ProxyApplyEmptyResult";

    /**
     * 缺少 apply 状态原因码。
     */
    public static final String REASON_MISSING_APPLY_STATE = "MissingApplyState";

    /**
     * proxy 空结果说明。
     */
    public static final String MESSAGE_PROXY_APPLY_EMPTY_RESULT = "proxy apply 未返回结果";

    /**
     * 缺少 apply 状态说明。
     */
    public static final String MESSAGE_MISSING_APPLY_STATE = "proxy apply 结果缺少状态";

    private AgentApplyConstants() {
        // agent apply 常量不允许实例化
    }
}
