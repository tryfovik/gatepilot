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

/**
 * agent WebClient 常量
 */
public final class AgentWebClientConstants {

    /**
     * Spring Cloud LoadBalancer 包名前缀
     */
    public static final String SPRING_CLOUD_LOADBALANCER_PACKAGE =
            "org.springframework.cloud.client.loadbalancer";

    /**
     * LoadBalancer filter 类名片段
     */
    public static final String LOAD_BALANCER_FILTER_CLASS_FRAGMENT = "LoadBalancer";

    /**
     * 禁止实例化
     */
    private AgentWebClientConstants() {
        // agent WebClient 常量不允许实例化
    }
}
