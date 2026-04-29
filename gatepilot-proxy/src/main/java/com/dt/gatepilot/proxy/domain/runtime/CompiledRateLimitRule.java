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

import java.util.Objects;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * proxy 运行态限流规则。
 */
@Data
public class CompiledRateLimitRule {

    /**
     * 限流器名称前缀。
     */
    private String limiterNamePrefix;

    /**
     * 参数来源。
     */
    private String source;

    /**
     * 参数名称。
     */
    private String name;

    /**
     * 固定参数值。
     */
    private String value;

    /**
     * 每秒允许请求数。
     */
    private int requestsPerSecond;

    /**
     * 判断规则是否命中当前请求。
     *
     * @param request 限流请求
     * @return 是否命中
     */
    public boolean matches(RateLimitRequest request) {
        if (!StringUtils.hasText(source)) {
            return true;
        }
        String actualValue = request.value(source, name);
        if (!StringUtils.hasText(actualValue)) {
            return false;
        }
        return !StringUtils.hasText(value) || value.equals(actualValue);
    }

    /**
     * 解析本次请求使用的限流器名称。
     *
     * @param request 限流请求
     * @return 限流器名称
     */
    public String limiterName(RateLimitRequest request) {
        if (!StringUtils.hasText(source) || StringUtils.hasText(value)) {
            return limiterNamePrefix;
        }
        String actualValue = request.value(source, name);
        return limiterNamePrefix + ProxyRateLimitConstants.LIMITER_KEY_SEPARATOR + Objects.toString(actualValue, "");
    }
}
