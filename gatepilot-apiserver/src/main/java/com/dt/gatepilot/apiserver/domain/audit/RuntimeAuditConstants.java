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
package com.dt.gatepilot.apiserver.domain.audit;

/**
 * 运行审计常量。
 */
public final class RuntimeAuditConstants {

    /**
     * 默认分页条数。
     */
    public static final int DEFAULT_LIMIT = 50;

    /**
     * 默认分页条数字符串。
     */
    public static final String DEFAULT_LIMIT_TEXT = "50";

    /**
     * 最大分页条数。
     */
    public static final int MAX_LIMIT = 500;

    /**
     * 空游标。
     */
    public static final long EMPTY_CURSOR = 0L;

    /**
     * 审计上报成功提示。
     */
    public static final String MESSAGE_AUDIT_ACCEPTED = "运行审计已接收";

    private RuntimeAuditConstants() {
        // 运行审计常量不允许实例化
    }
}
