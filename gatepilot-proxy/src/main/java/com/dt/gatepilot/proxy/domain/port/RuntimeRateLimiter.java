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
