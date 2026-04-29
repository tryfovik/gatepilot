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
package com.dt.gatepilot.proxy.domain.port;

import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;

/**
 * 运行时限流端口。
 */
public interface RuntimeRateLimiter {

    /**
     * 尝试获取限流许可。
     *
     * @param limiterName 限流器名称
     * @param rule 限流规则
     * @return 限流许可申请结果
     */
    RateLimitAcquireResult tryAcquire(String limiterName, CompiledRateLimitRule rule);
}
