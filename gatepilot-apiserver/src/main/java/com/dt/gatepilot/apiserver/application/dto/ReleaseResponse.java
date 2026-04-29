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
package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import lombok.Data;

/**
 * 发布请求响应。
 */
@Data
public class ReleaseResponse {

    /**
     * 发布请求标识。
     */
    private String releaseId;

    /**
     * 目标发布版本。
     */
    private String version;

    /**
     * 发布状态。
     */
    private String phase;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 创建时间。
     */
    private Instant createdAt;
}
