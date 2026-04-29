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
package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * proxy 上游负载均衡常量。
 */
public final class ProxyLoadBalanceConstants {

    /**
     * 轮询策略。
     */
    public static final String STRATEGY_ROUND_ROBIN = LoadBalanceStrategy.ROUND_ROBIN.name();

    /**
     * 默认负载均衡策略。
     */
    public static final String DEFAULT_STRATEGY = STRATEGY_ROUND_ROBIN;

    /**
     * 加权轮询策略。
     */
    public static final String STRATEGY_WEIGHTED_ROUND_ROBIN = LoadBalanceStrategy.WEIGHTED_ROUND_ROBIN.name();

    /**
     * 随机策略。
     */
    public static final String STRATEGY_RANDOM = LoadBalanceStrategy.RANDOM.name();

    /**
     * 支持的运行态策略。
     */
    public static final Set<String> SUPPORTED_STRATEGIES = Arrays.stream(LoadBalanceStrategy.values())
            .filter(LoadBalanceStrategy::isSupported)
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * 不支持负载均衡策略错误前缀。
     */
    public static final String ERROR_UNSUPPORTED_STRATEGY_PREFIX = "unsupported load balance strategy: ";

    /**
     * 最小端点权重。
     */
    public static final int MIN_ENDPOINT_WEIGHT = 1;

    /**
     * 隐藏工具类构造器。
     */
    private ProxyLoadBalanceConstants() {
        // proxy 负载均衡常量不允许实例化
    }
}
