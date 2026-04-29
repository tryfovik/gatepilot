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
package com.dt.gatepilot.proxy.domain.port;

/**
 * proxy 运行时认证结果。
 */
public record RuntimeAuthResult(boolean allowed,
                                int status,
                                int code,
                                String message,
                                String reason,
                                Throwable error) {

    /**
     * 创建认证通过结果。
     *
     * @return 认证结果
     */
    public static RuntimeAuthResult pass() {
        return new RuntimeAuthResult(true, 0, 0, null, null, null);
    }

    /**
     * 创建认证拒绝结果。
     *
     * @param status HTTP 状态码
     * @param code 业务码
     * @param message 提示
     * @param reason 原因码
     * @param error 异常
     * @return 认证结果
     */
    public static RuntimeAuthResult denied(int status,
                                           int code,
                                           String message,
                                           String reason,
                                           Throwable error) {
        return new RuntimeAuthResult(false, status, code, message, reason, error);
    }
}
