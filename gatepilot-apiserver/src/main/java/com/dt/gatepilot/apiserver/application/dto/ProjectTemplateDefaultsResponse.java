package com.dt.gatepilot.apiserver.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 项目接入模板默认配置响应
 */
@Data
public class ProjectTemplateDefaultsResponse {

    /**
     * 默认 values
     */
    private ProjectTemplateRenderRequest values;

    /**
     * 环境选项
     */
    private List<OptionItem> environments = new ArrayList<>();

    /**
     * 协议选项
     */
    private List<OptionItem> protocols = new ArrayList<>();

    /**
     * 负载均衡选项
     */
    private List<OptionItem> loadBalances = new ArrayList<>();

    /**
     * 发布策略选项
     */
    private List<OptionItem> releaseStrategies = new ArrayList<>();

    /**
     * 认证类型选项
     */
    private List<OptionItem> authTypes = new ArrayList<>();

    /**
     * 表单选项
     */
    @Data
    public static class OptionItem {

        /**
         * 选项值
         */
        private String value;

        /**
         * 展示文案
         */
        private String label;

        /**
         * 是否可选
         */
        private boolean enabled = true;
    }
}
