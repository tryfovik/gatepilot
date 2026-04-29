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
