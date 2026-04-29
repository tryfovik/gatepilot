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
import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.application.dto.AgentNodeProfile;
import com.dt.gatepilot.agent.application.dto.AgentRuntimeAuditBatch;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * agent 访问控制面的客户端扩展点。
 */
public interface AgentControlPlaneClient {

    /**
     * 注册节点。
     *
     * @param profile 节点身份信息
     */
    void register(AgentNodeProfile profile);

    /**
     * 发送心跳。
     *
     * @param heartbeat 心跳快照
     */
    void heartbeat(AgentHeartbeatSnapshot heartbeat);

    /**
     * 拉取已发布配置。
     *
     * @param cursor 本地配置游标
     * @return 最新配置
     */
    Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor);

    /**
     * 上报配置应用结果。
     *
     * @param result 应用结果
     */
    void reportApplyResult(AgentApplyResult result);

    /**
     * 上报运行审计批次。
     *
     * @param batch 运行审计批次
     */
    void reportRuntimeAudits(AgentRuntimeAuditBatch batch);
}
