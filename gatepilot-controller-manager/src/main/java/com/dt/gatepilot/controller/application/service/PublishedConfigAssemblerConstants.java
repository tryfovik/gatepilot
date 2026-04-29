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
package com.dt.gatepilot.controller.application.service;

/**
 * PublishedConfig 组装常量。
 */
public final class PublishedConfigAssemblerConstants {

    /**
     * 名称分隔符。
     */
    public static final String NAME_SEPARATOR = "-";

    /**
     * 资源名安全字符替换正则。
     */
    public static final String SAFE_NAME_REGEX = "[^a-zA-Z0-9._-]";

    /**
     * hash 字段分隔符。
     */
    public static final String HASH_FIELD_SEPARATOR = "|";

    /**
     * hash 列表分隔符。
     */
    public static final String HASH_LIST_SEPARATOR = ",";

    /**
     * 空 hash 片段。
     */
    public static final String EMPTY_HASH_PART = "";

    /**
     * SHA-256 摘要算法。
     */
    public static final String DIGEST_SHA_256 = "SHA-256";

    /**
     * 不支持负载均衡策略错误前缀。
     */
    public static final String ERROR_UNSUPPORTED_LOAD_BALANCE_PREFIX = "unsupported load balance strategy: ";

    /**
     * 缺少注册中心引用错误前缀。
     */
    public static final String ERROR_REGISTRY_REF_MISSING_PREFIX = "missing registry ref: ";

    /**
     * 注册中心不存在错误前缀。
     */
    public static final String ERROR_REGISTRY_CENTER_MISSING_PREFIX = "registry center not found: ";

    /**
     * 错误详情分隔符。
     */
    public static final String ERROR_DETAIL_SEPARATOR = " -> ";

    private PublishedConfigAssemblerConstants() {
        // PublishedConfig 组装常量不允许实例化
    }
}
