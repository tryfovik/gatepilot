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
     * 单次 reconcile 最大处理数量。
     */
    private int batchSize = 20;
}
