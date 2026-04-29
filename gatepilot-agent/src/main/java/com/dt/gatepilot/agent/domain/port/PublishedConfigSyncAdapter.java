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

import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * 已发布配置同步适配端口。
 */
public interface PublishedConfigSyncAdapter {

    /**
     * 按节点游标同步已发布配置。
     *
     * @param cursor 本地配置游标
     * @return 最新配置
     */
    Optional<PublishedConfig> sync(AgentConfigCursor cursor);
}
