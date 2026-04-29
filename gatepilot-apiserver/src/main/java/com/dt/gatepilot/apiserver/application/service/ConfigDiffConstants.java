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
package com.dt.gatepilot.apiserver.application.service;

/**
 * 配置 diff 常量。
 */
public final class ConfigDiffConstants {

    /**
     * 路由资源类型。
     */
    public static final String RESOURCE_ROUTE = "route";

    /**
     * 上游资源类型。
     */
    public static final String RESOURCE_UPSTREAM = "upstream";

    /**
     * 策略资源类型。
     */
    public static final String RESOURCE_POLICY = "policy";

    /**
     * 新增变更类型。
     */
    public static final String CHANGE_ADDED = "ADDED";

    /**
     * 删除变更类型。
     */
    public static final String CHANGE_REMOVED = "REMOVED";

    /**
     * 修改变更类型。
     */
    public static final String CHANGE_CHANGED = "CHANGED";

    /**
     * 未知名称。
     */
    public static final String UNKNOWN_NAME = "unknown";

    /**
     * 资源 key 分隔符。
     */
    public static final String RESOURCE_KEY_SEPARATOR = "/";

    /**
     * SHA-256 摘要算法。
     */
    public static final String DIGEST_SHA_256 = "SHA-256";

    /**
     * 快照名称分隔符。
     */
    public static final String SNAPSHOT_NAME_SEPARATOR = "-";

    /**
     * 资源名安全字符替换正则。
     */
    public static final String SAFE_NAME_REGEX = "[^a-zA-Z0-9._-]";

    private ConfigDiffConstants() {
        // 配置 diff 常量不允许实例化
    }
}
