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
package com.dt.gatepilot.proxy.infrastructure.limiter;

import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;
import com.getboot.limiter.api.model.LimiterAlgorithm;
import com.getboot.limiter.api.model.LimiterRule;
import com.getboot.limiter.api.registry.RateLimiterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 基于 getboot-limiter 的运行时限流适配器。
 */
public class GetbootRuntimeRateLimiter implements RuntimeRateLimiter {

    /**
     * getboot 限流注册表提供器。
     */
    private final ObjectProvider<RateLimiterRegistry> registryProvider;

    /**
     * 创建运行时限流适配器。
     *
     * @param registryProvider getboot 限流注册表提供器
     */
    public GetbootRuntimeRateLimiter(ObjectProvider<RateLimiterRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    /**
     * 尝试获取限流许可。
     *
     * @param limiterName 限流器名称
     * @param rule 限流规则
     * @return 限流许可申请结果
     */
    @Override
    public RateLimitAcquireResult tryAcquire(String limiterName, CompiledRateLimitRule rule) {
        RateLimiterRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return RateLimitAcquireResult.UNAVAILABLE;
        }
        try {
            // 令牌桶没有滑动窗口锁竞争，更适合作为网关入口默认限流算法
            boolean acquired = registry.tryAcquire(
                    limiterName,
                    limiterRule(rule),
                    ProxyRateLimitConstants.DEFAULT_PERMITS,
                    ProxyRateLimitConstants.DEFAULT_TIMEOUT,
                    TimeUnit.MILLISECONDS
            );
            return acquired ? RateLimitAcquireResult.ALLOWED : RateLimitAcquireResult.REJECTED;
        } catch (RuntimeException exception) {
            return RateLimitAcquireResult.UNAVAILABLE;
        }
    }

    /**
     * 转换 getboot 限流规则。
     *
     * @param rule 限流规则
     * @return getboot 限流规则
     */
    private LimiterRule limiterRule(CompiledRateLimitRule rule) {
        LimiterRule limiterRule = new LimiterRule();
        limiterRule.setAlgorithm(LimiterAlgorithm.TOKEN_BUCKET);
        limiterRule.setRate(rule.getRequestsPerSecond());
        limiterRule.setInterval(ProxyRateLimitConstants.DEFAULT_INTERVAL);
        limiterRule.setIntervalUnit(ProxyRateLimitConstants.DEFAULT_INTERVAL_UNIT);
        return limiterRule;
    }
}
