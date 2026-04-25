package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.infrastructure.config.GatePilotControllerManagerProperties;
import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 发布意图定时 reconcile 调度器。
 */
public class ReleaseReconcileScheduler {

    private final GatePilotControllerManagerProperties properties;

    private final ReleaseReconcileController reconcileController;

    /**
     * 创建发布意图定时 reconcile 调度器。
     *
     * @param properties controller-manager 配置
     * @param reconcileController 发布意图 reconcile 控制器
     */
    public ReleaseReconcileScheduler(GatePilotControllerManagerProperties properties,
                                     ReleaseReconcileController reconcileController) {
        this.properties = properties;
        this.reconcileController = reconcileController;
    }

    /**
     * 定时推进待发布意图。
     */
    @Scheduled(fixedDelayString = "${gatepilot.controller-manager.reconcile-interval-ms:5000}")
    public void reconcilePendingReleaseIntents() {
        if (!properties.isEnabled()) {
            return;
        }
        reconcileController.reconcileBatch(properties.getBatchSize());
    }
}
