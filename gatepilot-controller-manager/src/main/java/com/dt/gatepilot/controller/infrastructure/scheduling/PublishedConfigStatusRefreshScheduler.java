package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.dt.gatepilot.controller.infrastructure.config.GatePilotControllerManagerProperties;
import com.getboot.lock.api.exception.DistributedLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * PublishedConfig 状态刷新调度器
 */
public class PublishedConfigStatusRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(PublishedConfigStatusRefreshScheduler.class);

    private final GatePilotControllerManagerProperties properties;

    private final PublishedConfigStatusRefreshExecutor refreshExecutor;

    /**
     * 创建 PublishedConfig 状态刷新调度器
     *
     * @param properties controller-manager 配置
     * @param refreshExecutor 状态刷新执行器
     */
    public PublishedConfigStatusRefreshScheduler(GatePilotControllerManagerProperties properties,
                                                 PublishedConfigStatusRefreshExecutor refreshExecutor) {
        this.properties = properties;
        this.refreshExecutor = refreshExecutor;
    }

    /**
     * 定时刷新 PublishedConfig 应用状态
     */
    @Scheduled(fixedDelayString = ControllerManagerConstants.STATUS_REFRESH_INTERVAL_PLACEHOLDER)
    public void refreshPublishedConfigStatus() {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            refreshExecutor.refreshBatch(properties.getBatchSize());
        } catch (DistributedLockException exception) {
            // 多副本下抢不到锁是正常 standby 行为
            log.debug(ControllerManagerConstants.MESSAGE_STATUS_REFRESH_LOCK_BUSY, exception);
        }
    }
}
