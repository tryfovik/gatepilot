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
package com.dt.gatepilot.domain.resource.platform;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 控制面参数资源，用于统一保存可配置的后端参数
 */
@Data
public class ControlPlaneSetting {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private ControlPlaneSettingSpec spec = new ControlPlaneSettingSpec();

    /**
     * 当前状态
     */
    private ControlPlaneSettingStatus status = new ControlPlaneSettingStatus();

    /**
     * 控制面参数期望状态
     */
    @Data
    public static class ControlPlaneSettingSpec {

        /**
         * 参数所属模块
         */
        private String module;

        /**
         * Spring 配置键
         */
        private String key;

        /**
         * 参数值类型
         */
        private String valueType;

        /**
         * 参数值
         */
        private String value;

        /**
         * 默认值
         */
        private String defaultValue;

        /**
         * 参数说明
         */
        private String description;

        /**
         * 是否启用
         */
        private Boolean enabled = true;

        /**
         * 是否支持热生效
         */
        private Boolean hotReloadable = true;

        /**
         * 生效模式，例如 immediate、watch、scheduled
         */
        private String applyMode;

        /**
         * 生效范围，例如 apiserver、controller-manager、agent、proxy
         */
        private String scope;

        /**
         * 参数扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 控制面参数实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ControlPlaneSettingStatus extends ResourceStatus {

        /**
         * 最近应用时间
         */
        private Instant lastAppliedAt;

        /**
         * 当前生效值
         */
        private String appliedValue;

        /**
         * 最近应用版本
         */
        private Long appliedGeneration;
    }
}
