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

import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.getboot.lock.api.annotation.DistributedLock;

/**
 * 发布 reconcile 执行器。
 */
public class ReleaseReconcileExecutor {

    private final ReleaseReconcileController reconcileController;

    /**
     * 创建发布 reconcile 执行器。
     *
     * @param reconcileController 发布意图 reconcile 控制器
     */
    public ReleaseReconcileExecutor(ReleaseReconcileController reconcileController) {
        this.reconcileController = reconcileController;
    }

    /**
     * 在分布式锁保护下推进待发布意图。
     *
     * @param batchSize 单批处理数量
     * @return 已处理数量
     */
    @DistributedLock(
            scene = ControllerManagerConstants.RECONCILE_LOCK_SCENE,
            key = ControllerManagerConstants.RECONCILE_LOCK_KEY,
            waitTime = ControllerManagerConstants.RECONCILE_LOCK_WAIT_TIME_MS
    )
    public int reconcileBatch(int batchSize) {
        // 分布式锁保护批次入口，单条意图仍由 claim 兜底
        return reconcileController.reconcileBatch(batchSize);
    }
}
