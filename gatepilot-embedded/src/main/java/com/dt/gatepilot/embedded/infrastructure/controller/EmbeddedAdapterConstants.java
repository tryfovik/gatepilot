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
package com.dt.gatepilot.embedded.infrastructure.controller;

/**
 * 嵌入式适配器常量。
 */
public final class EmbeddedAdapterConstants {

    /**
     * 单页资源读取上限。
     */
    public static final int RESOURCE_PAGE_LIMIT = 500;

    /**
     * reconcile 完成消息。
     */
    public static final String MESSAGE_RECONCILE_COMPLETED = "controller-manager 已生成 PublishedConfig 并保存快照";

    /**
     * 回滚目标版本缺失消息。
     */
    public static final String MESSAGE_ROLLBACK_TARGET_VERSION_REQUIRED = "回滚目标版本不能为空";

    /**
     * 回滚快照缺失消息。
     */
    public static final String MESSAGE_ROLLBACK_SNAPSHOT_MISSING = "目标回滚快照不存在";

    /**
     * 回滚配置缺失消息。
     */
    public static final String MESSAGE_ROLLBACK_CONFIG_MISSING = "目标回滚快照缺少 PublishedConfig";

    /**
     * 回滚配置哈希不匹配消息。
     */
    public static final String MESSAGE_ROLLBACK_CONFIG_HASH_MISMATCH = "目标回滚快照哈希不匹配";

    private EmbeddedAdapterConstants() {
        // 嵌入式适配器常量不允许实例化
    }
}
