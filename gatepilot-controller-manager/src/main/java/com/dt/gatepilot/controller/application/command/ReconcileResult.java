package com.dt.gatepilot.controller.application.command;

import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import lombok.Data;

/**
 * 发布 reconcile 结果。
 */
@Data
public class ReconcileResult {

    /**
     * 生成的已发布配置。
     */
    private PublishedConfig publishedConfig;

    /**
     * 发布事件。
     */
    private GatewayEvent event;
}
