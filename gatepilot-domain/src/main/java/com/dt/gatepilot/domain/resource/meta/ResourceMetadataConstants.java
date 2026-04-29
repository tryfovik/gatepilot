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
package com.dt.gatepilot.domain.resource.meta;

/**
 * GatePilot 资源元信息常量。
 */
public final class ResourceMetadataConstants {

    /**
     * 默认命名空间。
     */
    public static final String DEFAULT_NAMESPACE = "default";

    /**
     * 平台资源命名空间。
     */
    public static final String SYSTEM_NAMESPACE = "system";

    /**
     * GatePilot 标签前缀。
     */
    public static final String LABEL_PREFIX = "gatepilot.io/";

    /**
     * 项目标签。
     */
    public static final String LABEL_PROJECT = LABEL_PREFIX + "project";

    /**
     * 配置分片标签。
     */
    public static final String LABEL_CONFIG_SHARD = LABEL_PREFIX + "config-shard";

    /**
     * 配置版本标签。
     */
    public static final String LABEL_VERSION = LABEL_PREFIX + "version";

    /**
     * 目标版本标签。
     */
    public static final String LABEL_TARGET_VERSION = LABEL_PREFIX + "target-version";

    /**
     * 节点标识标签。
     */
    public static final String LABEL_NODE_ID = LABEL_PREFIX + "node-id";

    /**
     * 节点角色标签。
     */
    public static final String LABEL_ROLE = LABEL_PREFIX + "role";

    /**
     * 事件类型标签。
     */
    public static final String LABEL_EVENT_TYPE = LABEL_PREFIX + "event-type";

    /**
     * reconcile 状态标签。
     */
    public static final String LABEL_RECONCILE_STATE = LABEL_PREFIX + "reconcile-state";

    /**
     * reconcile 控制器标签。
     */
    public static final String LABEL_RECONCILE_CONTROLLER = LABEL_PREFIX + "reconcile-controller";

    private ResourceMetadataConstants() {
        // 元信息常量不允许实例化
    }
}
