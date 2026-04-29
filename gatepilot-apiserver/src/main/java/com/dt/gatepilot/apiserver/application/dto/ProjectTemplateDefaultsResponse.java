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
