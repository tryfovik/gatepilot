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
package com.dt.gatepilot.agent.infrastructure.http;

import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * agent WebClient 构建工具
 */
public final class AgentWebClients {

    /**
     * 禁止实例化
     */
    private AgentWebClients() {
        // agent WebClient 工具不允许实例化
    }

    /**
     * 创建普通 HTTP WebClient
     *
     * @param builder WebClient 构建器
     * @param baseUrl 基础地址
     * @return WebClient
     */
    public static WebClient httpClient(WebClient.Builder builder, String baseUrl) {
        return builder.clone()
                .filters(filters -> filters.removeIf(AgentWebClients::loadBalancerFilter))
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 判断是否为 LoadBalancer filter
     *
     * @param filter WebClient filter
     * @return true 表示需要移除
     */
    private static boolean loadBalancerFilter(ExchangeFilterFunction filter) {
        String className = filter.getClass().getName();
        return className.startsWith(AgentWebClientConstants.SPRING_CLOUD_LOADBALANCER_PACKAGE)
                && className.contains(AgentWebClientConstants.LOAD_BALANCER_FILTER_CLASS_FRAGMENT);
    }
}
