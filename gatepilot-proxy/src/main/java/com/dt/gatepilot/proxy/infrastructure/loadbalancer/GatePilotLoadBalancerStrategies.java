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
package com.dt.gatepilot.proxy.infrastructure.loadbalancer;

import com.dt.gatepilot.proxy.domain.runtime.ProxyLoadBalanceConstants;
import java.util.Locale;

/**
 * GatePilot 负载均衡策略工具
 */
public final class GatePilotLoadBalancerStrategies {

    private GatePilotLoadBalancerStrategies() {
        // 负载均衡策略工具不允许实例化
    }

    /**
     * 归一化负载均衡策略
     *
     * @param strategy 策略文本
     * @return 归一化策略
     */
    public static String normalize(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return ProxyLoadBalanceConstants.DEFAULT_STRATEGY;
        }
        return strategy.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * 判断是否随机策略
     *
     * @param strategy 策略文本
     * @return 是否随机策略
     */
    public static boolean random(String strategy) {
        return ProxyLoadBalanceConstants.STRATEGY_RANDOM.equals(normalize(strategy));
    }

    /**
     * 判断是否加权轮询策略
     *
     * @param strategy 策略文本
     * @return 是否加权轮询策略
     */
    public static boolean weightedRoundRobin(String strategy) {
        return ProxyLoadBalanceConstants.STRATEGY_WEIGHTED_ROUND_ROBIN.equals(normalize(strategy));
    }
}
