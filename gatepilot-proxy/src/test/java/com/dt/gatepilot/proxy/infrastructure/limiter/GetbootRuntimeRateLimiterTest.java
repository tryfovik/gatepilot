package com.dt.gatepilot.proxy.infrastructure.limiter;

import com.dt.gatepilot.proxy.domain.runtime.CompiledRateLimitRule;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRateLimitConstants;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitAcquireResult;
import com.getboot.limiter.api.model.LimiterAlgorithm;
import com.getboot.limiter.api.model.LimiterRule;
import com.getboot.limiter.api.registry.RateLimiterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;

class GetbootRuntimeRateLimiterTest {

    @Test
    void shouldUseTokenBucketForGatewayRuntimeRateLimiting() {
        CapturingRateLimiterRegistry registry = new CapturingRateLimiterRegistry();
        StaticObjectProvider provider = new StaticObjectProvider(registry);
        CompiledRateLimitRule rule = new CompiledRateLimitRule();
        rule.setRequestsPerSecond(1000);
        rule.setLimiterNamePrefix("qa-shop:traffic:route");

        RateLimitAcquireResult result = new GetbootRuntimeRateLimiter(provider)
                .tryAcquire("qa-shop:traffic:route", rule);

        assertThat(result).isEqualTo(RateLimitAcquireResult.ALLOWED);
        assertThat(registry.limiterName).isEqualTo("qa-shop:traffic:route");
        assertThat(registry.permits).isEqualTo(ProxyRateLimitConstants.DEFAULT_PERMITS);
        assertThat(registry.timeout).isEqualTo(ProxyRateLimitConstants.DEFAULT_TIMEOUT);
        assertThat(registry.timeUnit).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(registry.rule.getAlgorithm()).isEqualTo(LimiterAlgorithm.TOKEN_BUCKET);
        assertThat(registry.rule.getRate()).isEqualTo(1000);
    }

    private static final class StaticObjectProvider implements ObjectProvider<RateLimiterRegistry> {

        private final RateLimiterRegistry registry;

        private StaticObjectProvider(RateLimiterRegistry registry) {
            this.registry = registry;
        }

        @Override
        public RateLimiterRegistry getObject(Object... args) {
            return registry;
        }

        @Override
        public RateLimiterRegistry getIfAvailable() {
            return registry;
        }

        @Override
        public RateLimiterRegistry getIfUnique() {
            return registry;
        }

        @Override
        public RateLimiterRegistry getObject() {
            return registry;
        }
    }

    private static final class CapturingRateLimiterRegistry implements RateLimiterRegistry {

        private String limiterName;

        private LimiterRule rule;

        private long permits;

        private long timeout;

        private TimeUnit timeUnit;

        @Override
        public boolean tryAcquire(String limiterName,
                                  LimiterRule rule,
                                  long permits,
                                  long timeout,
                                  TimeUnit timeUnit) {
            this.limiterName = limiterName;
            this.rule = rule;
            this.permits = permits;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            return true;
        }

        @Override
        public void configureRateLimiter(String limiterName, LimiterRule config) {
        }

        @Override
        public boolean tryAcquire(String limiterName) {
            return false;
        }

        @Override
        public boolean tryAcquire(String limiterName, long permits) {
            return false;
        }

        @Override
        public boolean tryAcquire(String limiterName, long timeout, TimeUnit timeUnit) {
            return false;
        }

        @Override
        public boolean tryAcquire(String limiterName, long permits, long timeout, TimeUnit timeUnit) {
            return false;
        }

        @Override
        public void updateRateLimiterConfig(String limiterName, LimiterRule newConfig) {
        }

        @Override
        public boolean deleteRateLimiter(String limiterName) {
            return false;
        }
    }
}
