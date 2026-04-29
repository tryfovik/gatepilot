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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 运行态熔断策略。
 */
@Data
public class CompiledCircuitBreakerPolicy {

    /**
     * 熔断器名称。
     */
    private String name;

    /**
     * 是否启用。
     */
    private boolean enabled;

    /**
     * 滑动窗口大小。
     */
    private int slidingWindowSize = ProxyCircuitBreakerConstants.DEFAULT_SLIDING_WINDOW_SIZE;

    /**
     * 熔断窗口内最小请求数。
     */
    private int minimumNumberOfCalls = ProxyCircuitBreakerConstants.DEFAULT_MINIMUM_NUMBER_OF_CALLS;

    /**
     * 失败率阈值。
     */
    private int failureRateThreshold = ProxyCircuitBreakerConstants.DEFAULT_FAILURE_RATE_THRESHOLD;

    /**
     * 慢调用率阈值。
     */
    private int slowCallRateThreshold = ProxyCircuitBreakerConstants.DEFAULT_SLOW_CALL_RATE_THRESHOLD;

    /**
     * 慢调用阈值。
     */
    private Duration slowCallDurationThreshold;

    /**
     * 熔断打开后的等待时间。
     */
    private Duration waitDurationInOpenState = ProxyCircuitBreakerConstants.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

    /**
     * 半开状态允许的探测请求数。
     */
    private int permittedNumberOfCallsInHalfOpenState = ProxyCircuitBreakerConstants.DEFAULT_HALF_OPEN_CALLS;

    /**
     * 视为失败的 HTTP 状态码。
     */
    private List<Integer> statusCodes = new ArrayList<>(ProxyCircuitBreakerConstants.DEFAULT_FAILURE_STATUS_CODES);

    /**
     * fallback HTTP 状态。
     */
    private int fallbackStatus = ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_STATUS;

    /**
     * fallback 业务码。
     */
    private int fallbackCode = ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CODE;

    /**
     * fallback 提示。
     */
    private String fallbackMessage = ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_MESSAGE;

    /**
     * fallback 响应类型。
     */
    private String contentType = ProxyCircuitBreakerConstants.DEFAULT_FALLBACK_CONTENT_TYPE;
}
