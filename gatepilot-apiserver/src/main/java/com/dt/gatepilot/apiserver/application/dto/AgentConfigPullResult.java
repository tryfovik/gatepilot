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

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import lombok.Data;

/**
 * agent 拉取已发布配置响应。
 */
@Data
public class AgentConfigPullResult {

    /**
     * 是否存在新配置。
     */
    private boolean changed;

    /**
     * 最新已发布配置。
     */
    private PublishedConfig publishedConfig;

    /**
     * 控制面提示信息。
     */
    private String message;
}
