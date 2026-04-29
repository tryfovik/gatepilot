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

import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.getboot.lock.api.annotation.DistributedLock;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * controller-manager 分布式锁配置测试
 */
class ControllerManagerDistributedLockTest {

    @Test
    void shouldProtectReleaseReconcileBatchWithGetbootLock() throws NoSuchMethodException {
        DistributedLock lock = lockAnnotation(ReleaseReconcileExecutor.class, "reconcileBatch");

        assertThat(lock.scene()).isEqualTo(ControllerManagerConstants.RECONCILE_LOCK_SCENE);
        assertThat(lock.key()).isEqualTo(ControllerManagerConstants.RECONCILE_LOCK_KEY);
        assertThat(lock.waitTime()).isEqualTo(ControllerManagerConstants.RECONCILE_LOCK_WAIT_TIME_MS);
    }

    @Test
    void shouldProtectPublishedConfigStatusRefreshWithGetbootLock() throws NoSuchMethodException {
        DistributedLock lock = lockAnnotation(PublishedConfigStatusRefreshExecutor.class, "refreshBatch");

        assertThat(lock.scene()).isEqualTo(ControllerManagerConstants.RECONCILE_LOCK_SCENE);
        assertThat(lock.key()).isEqualTo(ControllerManagerConstants.STATUS_REFRESH_LOCK_KEY);
        assertThat(lock.waitTime()).isEqualTo(ControllerManagerConstants.RECONCILE_LOCK_WAIT_TIME_MS);
    }

    private DistributedLock lockAnnotation(Class<?> type, String methodName) throws NoSuchMethodException {
        Method method = type.getMethod(methodName, int.class);
        DistributedLock lock = method.getAnnotation(DistributedLock.class);
        assertThat(lock).isNotNull();
        return lock;
    }
}
