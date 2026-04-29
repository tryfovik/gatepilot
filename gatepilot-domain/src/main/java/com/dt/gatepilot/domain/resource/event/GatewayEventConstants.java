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
package com.dt.gatepilot.domain.resource.event;

/**
 * GatePilot 事件资源常量。
 */
public final class GatewayEventConstants {

    /**
     * 发布请求事件类型。
     */
    public static final String EVENT_TYPE_RELEASE_REQUEST = "release-request";

    /**
     * 回滚请求事件类型。
     */
    public static final String EVENT_TYPE_ROLLBACK_REQUEST = "rollback-request";

    /**
     * PublishedConfig 生成事件类型。
     */
    public static final String EVENT_TYPE_PUBLISHED_CONFIG_GENERATED = "published-config-generated";

    /**
     * 待处理状态。
     */
    public static final String RECONCILE_STATE_PENDING = "pending";

    /**
     * 处理中状态。
     */
    public static final String RECONCILE_STATE_PROCESSING = "processing";

    /**
     * 已完成状态。
     */
    public static final String RECONCILE_STATE_COMPLETED = "completed";

    /**
     * 已失败状态。
     */
    public static final String RECONCILE_STATE_FAILED = "failed";

    /**
     * apiserver 事件来源。
     */
    public static final String SOURCE_APISERVER = "apiserver";

    /**
     * controller-manager 事件来源。
     */
    public static final String SOURCE_CONTROLLER_MANAGER = "controller-manager";

    /**
     * 创建发布请求原因码。
     */
    public static final String REASON_CREATE_RELEASE_COMMANDED = "CreateReleaseCommanded";

    /**
     * 创建回滚请求原因码。
     */
    public static final String REASON_CREATE_ROLLBACK_COMMANDED = "CreateRollbackCommanded";

    /**
     * 发布已推进原因码。
     */
    public static final String REASON_RELEASE_RECONCILED = "ReleaseReconciled";

    /**
     * 回滚已推进原因码。
     */
    public static final String REASON_ROLLBACK_RECONCILED = "RollbackReconciled";

    /**
     * PublishedConfig 已生成原因码。
     */
    public static final String REASON_PUBLISHED_CONFIG_GENERATED = "PublishedConfigGenerated";

    /**
     * 回滚 PublishedConfig 已生成原因码。
     */
    public static final String REASON_ROLLBACK_CONFIG_GENERATED = "RollbackConfigGenerated";

    /**
     * 发布标识属性。
     */
    public static final String ATTRIBUTE_RELEASE_ID = "releaseId";

    /**
     * 版本属性。
     */
    public static final String ATTRIBUTE_VERSION = "version";

    /**
     * 项目名称属性。
     */
    public static final String ATTRIBUTE_PROJECT_NAME = "projectName";

    /**
     * 创建人属性。
     */
    public static final String ATTRIBUTE_CREATED_BY = "createdBy";

    /**
     * 说明属性。
     */
    public static final String ATTRIBUTE_DESCRIPTION = "description";

    /**
     * 目标版本属性。
     */
    public static final String ATTRIBUTE_TARGET_VERSION = "targetVersion";

    /**
     * 配置哈希属性。
     */
    public static final String ATTRIBUTE_CONFIG_HASH = "configHash";

    /**
     * 抢占控制器属性。
     */
    public static final String ATTRIBUTE_CLAIMED_BY = "claimedBy";

    /**
     * 抢占时间属性。
     */
    public static final String ATTRIBUTE_CLAIMED_AT = "claimedAt";

    /**
     * 已发布配置名称属性。
     */
    public static final String ATTRIBUTE_PUBLISHED_CONFIG_NAME = "publishedConfigName";

    /**
     * 完成时间属性。
     */
    public static final String ATTRIBUTE_COMPLETED_AT = "completedAt";

    /**
     * 失败时间属性。
     */
    public static final String ATTRIBUTE_FAILED_AT = "failedAt";

    /**
     * 配置分片属性。
     */
    public static final String ATTRIBUTE_CONFIG_SHARD = "configShard";

    /**
     * 发布触发类型。
     */
    public static final String TRIGGER_PUBLISH = "publish";

    /**
     * 回滚触发类型。
     */
    public static final String TRIGGER_ROLLBACK = "rollback";

    private GatewayEventConstants() {
        // 事件常量不允许实例化
    }
}
