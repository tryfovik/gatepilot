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
package com.dt.gatepilot.domain.enums;

/**
 * 上游负载均衡策略。
 */
public enum LoadBalanceStrategy {

    /**
     * 轮询。
     */
    ROUND_ROBIN,

    /**
     * 加权轮询。
     */
    WEIGHTED_ROUND_ROBIN,

    /**
     * 最少连接。
     */
    LEAST_CONNECTIONS,

    /**
     * 随机。
     */
    RANDOM,

    /**
     * 一致性哈希。
     */
    CONSISTENT_HASH;

    /**
     * 判断当前策略是否已有运行组件承接。
     *
     * @return 是否可在当前运行态执行
     */
    public boolean isSupported() {
        return switch (this) {
            case ROUND_ROBIN, WEIGHTED_ROUND_ROBIN, RANDOM -> true;
            case LEAST_CONNECTIONS, CONSISTENT_HASH -> false;
        };
    }
}
