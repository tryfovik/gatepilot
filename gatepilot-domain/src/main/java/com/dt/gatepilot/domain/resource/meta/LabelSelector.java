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
package com.dt.gatepilot.domain.resource.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 标签选择器，用于策略绑定一组资源。
 */
@Data
public class LabelSelector {

    /**
     * 精确匹配的标签集合。
     */
    private Map<String, String> matchLabels = new LinkedHashMap<>();

    /**
     * 表达式匹配条件。
     */
    private List<MatchExpression> matchExpressions = new ArrayList<>();

    /**
     * 标签表达式。
     */
    @Data
    public static class MatchExpression {

        /**
         * 标签键。
         */
        private String key;

        /**
         * 操作符，例如 In、NotIn、Exists、DoesNotExist。
         */
        private String operator;

        /**
         * 操作符需要的取值列表。
         */
        private List<String> values = new ArrayList<>();
    }
}
