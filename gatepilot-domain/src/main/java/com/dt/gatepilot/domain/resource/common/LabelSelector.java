package com.dt.gatepilot.domain.resource.common;

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
