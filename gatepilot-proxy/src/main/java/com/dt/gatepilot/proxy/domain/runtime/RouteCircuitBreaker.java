package com.dt.gatepilot.proxy.domain.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 路由熔断器，按已发布运行态在本机内存维护窗口状态。
 */
public class RouteCircuitBreaker {

    /**
     * 熔断器状态索引。
     */
    private final ConcurrentMap<String, CircuitState> states = new ConcurrentHashMap<>();

    /**
     * 尝试获取本次请求执行许可。
     *
     * @param policy 熔断策略
     * @param nowNanos 当前纳秒时间
     * @return 是否允许继续转发
     */
    public boolean tryAcquire(CompiledCircuitBreakerPolicy policy, long nowNanos) {
        CircuitState state = states.computeIfAbsent(policy.getName(), ignored -> new CircuitState());
        return state.tryAcquire(policy, nowNanos);
    }

    /**
     * 记录请求结果。
     *
     * @param policy 熔断策略
     * @param failure 是否失败
     * @param slow 是否慢调用
     * @param nowNanos 当前纳秒时间
     */
    public void record(CompiledCircuitBreakerPolicy policy, boolean failure, boolean slow, long nowNanos) {
        CircuitState state = states.computeIfAbsent(policy.getName(), ignored -> new CircuitState());
        state.record(policy, failure, slow, nowNanos);
    }

    /**
     * 判断状态码是否计入失败。
     *
     * @param policy 熔断策略
     * @param statusCode HTTP 状态码
     * @return 是否失败
     */
    public boolean failureStatus(CompiledCircuitBreakerPolicy policy, int statusCode) {
        return policy.getStatusCodes().contains(statusCode);
    }

    /**
     * 判断本次请求是否为慢调用。
     *
     * @param policy 熔断策略
     * @param startNanos 开始纳秒时间
     * @param nowNanos 当前纳秒时间
     * @return 是否慢调用
     */
    public boolean slowCall(CompiledCircuitBreakerPolicy policy, long startNanos, long nowNanos) {
        return policy.getSlowCallDurationThreshold() != null
                && nowNanos - startNanos >= policy.getSlowCallDurationThreshold().toNanos();
    }

    /**
     * 单个熔断器状态。
     */
    private static final class CircuitState {

        /**
         * 当前状态。
         */
        private CircuitStatus status = CircuitStatus.CLOSED;

        /**
         * 打开状态截止时间。
         */
        private long openUntilNanos;

        /**
         * 半开状态正在执行的探测请求数。
         */
        private int halfOpenActiveCalls;

        /**
         * 半开状态已完成的探测请求数。
         */
        private int halfOpenCompletedCalls;

        /**
         * 关闭状态滑动窗口。
         */
        private final Deque<CallOutcome> outcomes = new ArrayDeque<>();

        /**
         * 尝试获取请求许可。
         *
         * @param policy 熔断策略
         * @param nowNanos 当前纳秒时间
         * @return 是否允许继续转发
         */
        private synchronized boolean tryAcquire(CompiledCircuitBreakerPolicy policy, long nowNanos) {
            if (status == CircuitStatus.CLOSED) {
                return true;
            }
            if (status == CircuitStatus.OPEN && nowNanos >= openUntilNanos) {
                // 等待时间到期后进入半开探测
                status = CircuitStatus.HALF_OPEN;
                halfOpenActiveCalls = 0;
                halfOpenCompletedCalls = 0;
            }
            if (status == CircuitStatus.HALF_OPEN
                    && halfOpenActiveCalls < policy.getPermittedNumberOfCallsInHalfOpenState()) {
                halfOpenActiveCalls++;
                return true;
            }
            return false;
        }

        /**
         * 记录请求结果。
         *
         * @param policy 熔断策略
         * @param failure 是否失败
         * @param slow 是否慢调用
         * @param nowNanos 当前纳秒时间
         */
        private synchronized void record(CompiledCircuitBreakerPolicy policy,
                                         boolean failure,
                                         boolean slow,
                                         long nowNanos) {
            if (status == CircuitStatus.HALF_OPEN) {
                recordHalfOpen(policy, failure, slow, nowNanos);
                return;
            }
            if (status != CircuitStatus.CLOSED) {
                return;
            }
            // 关闭状态只维护固定长度滑动窗口
            outcomes.addLast(new CallOutcome(failure, slow));
            while (outcomes.size() > policy.getSlidingWindowSize()) {
                outcomes.removeFirst();
            }
            if (outcomes.size() < policy.getMinimumNumberOfCalls()) {
                return;
            }
            if (shouldOpen(policy)) {
                open(policy, nowNanos);
            }
        }

        /**
         * 记录半开状态请求结果。
         *
         * @param policy 熔断策略
         * @param failure 是否失败
         * @param slow 是否慢调用
         * @param nowNanos 当前纳秒时间
         */
        private void recordHalfOpen(CompiledCircuitBreakerPolicy policy,
                                    boolean failure,
                                    boolean slow,
                                    long nowNanos) {
            halfOpenActiveCalls = Math.max(0, halfOpenActiveCalls - 1);
            halfOpenCompletedCalls++;
            if (failure || slow) {
                open(policy, nowNanos);
                return;
            }
            if (halfOpenCompletedCalls >= policy.getPermittedNumberOfCallsInHalfOpenState()) {
                close();
            }
        }

        /**
         * 判断是否应该打开熔断。
         *
         * @param policy 熔断策略
         * @return 是否打开熔断
         */
        private boolean shouldOpen(CompiledCircuitBreakerPolicy policy) {
            int failures = 0;
            int slowCalls = 0;
            for (CallOutcome outcome : outcomes) {
                if (outcome.failure()) {
                    failures++;
                }
                if (outcome.slow()) {
                    slowCalls++;
                }
            }
            double failureRate = failures * ProxyCircuitBreakerConstants.PERCENT_BASE / outcomes.size();
            double slowCallRate = slowCalls * ProxyCircuitBreakerConstants.PERCENT_BASE / outcomes.size();
            return failureRate >= policy.getFailureRateThreshold()
                    || (policy.getSlowCallDurationThreshold() != null
                    && slowCallRate >= policy.getSlowCallRateThreshold());
        }

        /**
         * 打开熔断。
         *
         * @param policy 熔断策略
         * @param nowNanos 当前纳秒时间
         */
        private void open(CompiledCircuitBreakerPolicy policy, long nowNanos) {
            status = CircuitStatus.OPEN;
            openUntilNanos = nowNanos + policy.getWaitDurationInOpenState().toNanos();
            halfOpenActiveCalls = 0;
            halfOpenCompletedCalls = 0;
        }

        /**
         * 关闭熔断。
         */
        private void close() {
            status = CircuitStatus.CLOSED;
            outcomes.clear();
            halfOpenActiveCalls = 0;
            halfOpenCompletedCalls = 0;
        }
    }

    /**
     * 熔断状态。
     */
    private enum CircuitStatus {

        /**
         * 关闭。
         */
        CLOSED,

        /**
         * 打开。
         */
        OPEN,

        /**
         * 半开。
         */
        HALF_OPEN
    }

    /**
     * 单次调用结果。
     *
     * @param failure 是否失败
     * @param slow 是否慢调用
     */
    private record CallOutcome(boolean failure, boolean slow) {
    }
}
