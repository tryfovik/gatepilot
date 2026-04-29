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

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import java.time.Instant;
import lombok.Data;

/**
 * proxy 本机配置应用结果。
 */
@Data
public class ProxyApplyResult {

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 应用状态。
     */
    private ConfigApplyState state;

    /**
     * 开始时间。
     */
    private Instant startedAt;

    /**
     * 完成时间。
     */
    private Instant finishedAt;

    /**
     * 失败原因码。
     */
    private String reason;

    /**
     * 失败或结果说明。
     */
    private String message;

    /**
     * 创建成功结果。
     *
     * @param version 发布版本
     * @param configHash 配置哈希
     * @param startedAt 开始时间
     * @return 应用结果
     */
    public static ProxyApplyResult applied(String version, String configHash, Instant startedAt) {
        ProxyApplyResult result = new ProxyApplyResult();
        // 成功结果只表达 proxy 已切换运行态
        result.setVersion(version);
        result.setConfigHash(configHash);
        result.setState(ConfigApplyState.APPLIED);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        result.setMessage(ProxyApplyConstants.MESSAGE_APPLIED);
        return result;
    }

    /**
     * 创建失败结果。
     *
     * @param version 发布版本
     * @param configHash 配置哈希
     * @param reason 失败原因码
     * @param message 失败说明
     * @param startedAt 开始时间
     * @return 应用结果
     */
    public static ProxyApplyResult failed(String version,
                                          String configHash,
                                          String reason,
                                          String message,
                                          Instant startedAt) {
        ProxyApplyResult result = new ProxyApplyResult();
        // 失败结果保留版本和 hash，便于回溯发布产物
        result.setVersion(version);
        result.setConfigHash(configHash);
        result.setState(ConfigApplyState.FAILED);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        result.setReason(reason);
        result.setMessage(message);
        return result;
    }
}
