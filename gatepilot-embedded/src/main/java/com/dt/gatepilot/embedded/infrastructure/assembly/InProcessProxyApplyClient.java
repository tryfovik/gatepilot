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
package com.dt.gatepilot.embedded.infrastructure.assembly;

import com.dt.gatepilot.agent.application.dto.AgentApplyResult;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;

/**
 * 单体合包内的 proxy apply 客户端。
 */
public class InProcessProxyApplyClient implements ProxyApplyClient {

    private final ProxyConfigApplier proxyConfigApplier;

    /**
     * 创建进程内 proxy apply 客户端。
     *
     * @param proxyConfigApplier proxy 配置应用器
     */
    public InProcessProxyApplyClient(ProxyConfigApplier proxyConfigApplier) {
        this.proxyConfigApplier = proxyConfigApplier;
    }

    @Override
    public AgentApplyResult apply(PublishedConfig config) {
        // 单体部署直接走进程内调用，不经过 HTTP 绕一圈
        ProxyApplyRequest request = new ProxyApplyRequest();
        request.setPublishedConfig(config);
        ProxyApplyResult proxyResult = proxyConfigApplier.apply(request);
        return toAgentResult(proxyResult);
    }

    private AgentApplyResult toAgentResult(ProxyApplyResult proxyResult) {
        AgentApplyResult result = new AgentApplyResult();
        // agent 上报模型和 proxy 本机 apply 模型保持字段一一映射
        result.setVersion(proxyResult.getVersion());
        result.setConfigHash(proxyResult.getConfigHash());
        result.setState(proxyResult.getState());
        result.setStartedAt(proxyResult.getStartedAt());
        result.setFinishedAt(proxyResult.getFinishedAt());
        result.setReason(proxyResult.getReason());
        result.setMessage(proxyResult.getMessage());
        return result;
    }
}
