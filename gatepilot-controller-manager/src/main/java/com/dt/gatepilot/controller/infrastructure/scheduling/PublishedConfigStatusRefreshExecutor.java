package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.application.service.PublishedConfigStatusController;
import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.getboot.lock.api.annotation.DistributedLock;

/**
 * PublishedConfig 状态刷新执行器
 */
public class PublishedConfigStatusRefreshExecutor {

    private final PublishedConfigStatusController statusController;

    /**
     * 创建 PublishedConfig 状态刷新执行器
     *
     * @param statusController 状态刷新控制器
     */
    public PublishedConfigStatusRefreshExecutor(PublishedConfigStatusController statusController) {
        this.statusController = statusController;
    }

    /**
     * 在分布式锁保护下刷新 PublishedConfig 状态
     *
     * @param batchSize 单批刷新数量
     * @return 已刷新数量
     */
    @DistributedLock(
            scene = ControllerManagerConstants.RECONCILE_LOCK_SCENE,
            key = ControllerManagerConstants.STATUS_REFRESH_LOCK_KEY,
            waitTime = ControllerManagerConstants.RECONCILE_LOCK_WAIT_TIME_MS
    )
    public int refreshBatch(int batchSize) {
        // 状态刷新和发布推进分开加锁，避免长批次相互阻塞
        return statusController.refreshBatch(batchSize);
    }
}
