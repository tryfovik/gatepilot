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
package com.dt.gatepilot.demo.infrastructure.config;

import com.dt.gatepilot.demo.application.service.DemoEchoConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 示例上游配置
 */
@Data
@Component
@ConfigurationProperties(prefix = DemoUpstreamConstants.PROPERTIES_PREFIX)
public class DemoUpstreamProperties {

    /**
     * 服务名称
     */
    private String serviceName = DemoEchoConstants.DEFAULT_SERVICE_NAME;

    /**
     * 实例版本
     */
    private String version = DemoEchoConstants.DEFAULT_VERSION;

    /**
     * 流量颜色
     */
    private String color = DemoEchoConstants.DEFAULT_COLOR;

    /**
     * 实例标识
     */
    private String instanceId = DemoEchoConstants.DEFAULT_INSTANCE_ID;

    /**
     * 部署区域
     */
    private String zone = DemoEchoConstants.DEFAULT_ZONE;

    /**
     * 示例提示
     */
    private String message = DemoEchoConstants.DEFAULT_MESSAGE;
}
