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
