package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.dt.gatepilot.controller.infrastructure.config.GatePilotControllerManagerProperties;
import com.getboot.lock.api.exception.DistributedLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 发布意图定时 reconcile 调度器。
 */
public class ReleaseReconcileScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReleaseReconcileScheduler.class);

    private final GatePilotControllerManagerProperties properties;

    private final ReleaseReconcileExecutor reconcileExecutor;

    /**
     * 创建发布意图定时 reconcile 调度器。
     *
     * @param properties controller-manager 配置
     * @param reconcileExecutor 发布 reconcile 执行器
     */
    public ReleaseReconcileScheduler(GatePilotControllerManagerProperties properties,
                                     ReleaseReconcileExecutor reconcileExecutor) {
        this.properties = properties;
        this.reconcileExecutor = reconcileExecutor;
    }

    /**
     * 定时推进待发布意图。
     */
    @Scheduled(fixedDelayString = "${gatepilot.controller-manager.reconcile-interval-ms:5000}")
    public void reconcilePendingReleaseIntents() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            reconcileExecutor.reconcileBatch(properties.getBatchSize());
        } catch (DistributedLockException exception) {
            // 多副本下抢不到锁是正常 standby 行为
            log.debug(ControllerManagerConstants.MESSAGE_RECONCILE_LOCK_BUSY, exception);
        }
    }
}
