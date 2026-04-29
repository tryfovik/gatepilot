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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 运行态限流策略。
 */
@Data
public class CompiledRateLimitPolicy {

    /**
     * 策略名称。
     */
    private String name;

    /**
     * 是否启用。
     */
    private boolean enabled;

    /**
     * 限流规则列表。
     */
    private List<CompiledRateLimitRule> rules = new ArrayList<>();

    /**
     * 限流 HTTP 状态。
     */
    private int rejectStatus = ProxyRateLimitConstants.DEFAULT_REJECT_STATUS;

    /**
     * 限流业务码。
     */
    private int rejectCode = ProxyRateLimitConstants.DEFAULT_REJECT_CODE;

    /**
     * 限流提示。
     */
    private String rejectMessage = ProxyRateLimitConstants.DEFAULT_REJECT_MESSAGE;

    /**
     * 限流响应类型。
     */
    private String contentType = ProxyRateLimitConstants.DEFAULT_REJECT_CONTENT_TYPE;

    /**
     * 获取当前请求命中的限流规则。
     *
     * @param request 限流请求
     * @return 命中的限流规则
     */
    public List<CompiledRateLimitRule> matchingRules(RateLimitRequest request) {
        List<CompiledRateLimitRule> matchedRules = new ArrayList<>();
        for (CompiledRateLimitRule rule : rules) {
            if (rule.matches(request)) {
                matchedRules.add(rule);
            }
        }
        return matchedRules;
    }
}
