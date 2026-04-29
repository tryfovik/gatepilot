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

/**
 * proxy apply 结果常量。
 */
public final class ProxyApplyConstants {

    /**
     * 空配置原因码。
     */
    public static final String REASON_EMPTY_CONFIG = "EmptyConfig";

    /**
     * 缺少版本原因码。
     */
    public static final String REASON_MISSING_VERSION = "MissingVersion";

    /**
     * 缺少配置哈希原因码。
     */
    public static final String REASON_MISSING_CONFIG_HASH = "MissingConfigHash";

    /**
     * 编译失败原因码。
     */
    public static final String REASON_COMPILE_FAILED = "CompileFailed";

    /**
     * 治理规则发布失败原因码。
     */
    public static final String REASON_GOVERNANCE_RULE_PUBLISH_FAILED = "GovernanceRulePublishFailed";

    /**
     * 空配置说明。
     */
    public static final String MESSAGE_EMPTY_CONFIG = "PublishedConfig 不能为空";

    /**
     * 缺少版本说明。
     */
    public static final String MESSAGE_MISSING_VERSION = "PublishedConfig 缺少版本号";

    /**
     * 缺少配置哈希说明。
     */
    public static final String MESSAGE_MISSING_CONFIG_HASH = "PublishedConfig 缺少配置哈希";

    /**
     * 应用成功说明。
     */
    public static final String MESSAGE_APPLIED = "配置已应用";

    private ProxyApplyConstants() {
        // proxy apply 常量不允许实例化
    }
}
