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
package com.dt.gatepilot.controller.infrastructure.config;

/**
 * controller-manager 配置常量。
 */
public final class ControllerManagerConstants {

    /**
     * controller-manager 配置前缀。
     */
    public static final String CONFIG_PREFIX = "gatepilot.controller-manager";

    /**
     * controller-manager 启用开关配置名
     */
    public static final String ENABLED_PROPERTY = "enabled";

    /**
     * 分布式锁强校验配置名
     */
    public static final String DISTRIBUTED_LOCK_REQUIRED_PROPERTY = "distributed-lock-required";

    /**
     * 默认 controller 标识。
     */
    public static final String DEFAULT_CONTROLLER_ID = "local-controller";

    /**
     * 发布 reconcile 分布式锁场景。
     */
    public static final String RECONCILE_LOCK_SCENE = "gatepilot-controller-manager";

    /**
     * 发布 reconcile 分布式锁键。
     */
    public static final String RECONCILE_LOCK_KEY = "release-reconcile";

    /**
     * PublishedConfig 状态刷新分布式锁键
     */
    public static final String STATUS_REFRESH_LOCK_KEY = "published-config-status-refresh";

    /**
     * 发布 reconcile 锁等待时间。
     */
    public static final int RECONCILE_LOCK_WAIT_TIME_MS = 0;

    /**
     * 发布 reconcile 调度间隔占位符
     */
    public static final String RECONCILE_INTERVAL_PLACEHOLDER =
            "${gatepilot.controller-manager.reconcile-interval-ms:5000}";

    /**
     * PublishedConfig 状态刷新调度间隔占位符
     */
    public static final String STATUS_REFRESH_INTERVAL_PLACEHOLDER =
            "${gatepilot.controller-manager.status-refresh-interval-ms:5000}";

    /**
     * 发布 reconcile 锁占用提示。
     */
    public static final String MESSAGE_RECONCILE_LOCK_BUSY = "其他 controller-manager 正在推进发布，跳过本轮";

    /**
     * PublishedConfig 状态刷新锁占用提示
     */
    public static final String MESSAGE_STATUS_REFRESH_LOCK_BUSY =
            "其他 controller-manager 正在刷新发布状态，跳过本轮";

    /**
     * 缺失 getboot-lock 运行实现提示
     */
    public static final String MESSAGE_LOCK_IMPLEMENTATION_MISSING =
            "controller-manager 已启用但未发现 getboot-lock 分布式锁运行实现，请配置 Redis、database 或 ZooKeeper 锁";

    /**
     * getboot Redis 锁切面类型
     */
    public static final String GETBOOT_REDIS_LOCK_ASPECT_CLASS =
            "com.getboot.lock.infrastructure.redis.redisson.aspect.DistributedLockAspect";

    /**
     * getboot database 锁切面类型
     */
    public static final String GETBOOT_DATABASE_LOCK_ASPECT_CLASS =
            "com.getboot.lock.infrastructure.database.jdbc.aspect.JdbcDistributedLockAspect";

    /**
     * getboot ZooKeeper 锁切面类型
     */
    public static final String GETBOOT_ZOOKEEPER_LOCK_ASPECT_CLASS =
            "com.getboot.lock.infrastructure.zookeeper.curator.aspect.ZookeeperDistributedLockAspect";

    /**
     * getboot 已知锁切面类型集合
     */
    public static final String[] GETBOOT_LOCK_ASPECT_CLASSES = {
            GETBOOT_REDIS_LOCK_ASPECT_CLASS,
            GETBOOT_DATABASE_LOCK_ASPECT_CLASS,
            GETBOOT_ZOOKEEPER_LOCK_ASPECT_CLASS
    };

    private ControllerManagerConstants() {
        // controller-manager 配置常量不允许实例化
    }
}
