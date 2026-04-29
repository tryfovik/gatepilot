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
package com.dt.gatepilot.controller.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GatePilot controller-manager 配置。
 */
@Data
@ConfigurationProperties(prefix = ControllerManagerConstants.CONFIG_PREFIX)
public class GatePilotControllerManagerProperties {

    /**
     * 是否启用 controller-manager。
     */
    private boolean enabled = true;

    /**
     * controller-manager 实例标识。
     */
    private String controllerId = ControllerManagerConstants.DEFAULT_CONTROLLER_ID;

    /**
     * 是否要求启动时存在 getboot-lock 运行实现。
     */
    private boolean distributedLockRequired = true;

    /**
     * 单次 reconcile 最大处理数量。
     */
    private int batchSize = 20;
}
