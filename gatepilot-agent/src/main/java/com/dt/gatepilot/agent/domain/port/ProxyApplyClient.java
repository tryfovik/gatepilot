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
package com.dt.gatepilot.agent.domain.port;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;

/**
 * agent 调用本机 proxy 应用配置的客户端扩展点。
 */
public interface ProxyApplyClient {

    /**
     * 应用配置。
     *
     * @param config 已发布配置
     * @return 应用结果
     */
    AgentApplyResult apply(PublishedConfig config);
}
