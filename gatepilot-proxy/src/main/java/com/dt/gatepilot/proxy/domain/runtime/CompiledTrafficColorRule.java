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

import java.util.regex.Pattern;

/**
 * proxy 预编译流量染色规则。
 *
 * @param source 染色来源
 * @param fieldName 字段名称
 * @param pattern 匹配内容
 * @param matchStrategy 匹配策略
 * @param color 流量颜色
 * @param regexPattern 正则表达式
 */
public record CompiledTrafficColorRule(String source,
                                       String fieldName,
                                       String pattern,
                                       String matchStrategy,
                                       String color,
                                       Pattern regexPattern) {

    /**
     * 判断候选值是否命中规则。
     *
     * @param candidate 候选值
     * @return 是否命中
     */
    public boolean matches(String candidate) {
        return switch (matchStrategy) {
            case TrafficColorConstants.MATCH_PREFIX -> candidate.startsWith(pattern);
            case TrafficColorConstants.MATCH_CONTAINS -> candidate.contains(pattern);
            case TrafficColorConstants.MATCH_REGEX -> regexPattern != null && regexPattern.matcher(candidate)
                    .matches();
            default -> candidate.equals(pattern);
        };
    }
}
