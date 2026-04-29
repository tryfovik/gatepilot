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

import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;

/**
 * agent 读取本机 proxy 运行状态的端口。
 */
public interface ProxyRuntimeStatusReader {

    /**
     * 填充 proxy 运行状态。
     *
     * @param snapshot 心跳快照
     */
    void fill(AgentHeartbeatSnapshot snapshot);
}
