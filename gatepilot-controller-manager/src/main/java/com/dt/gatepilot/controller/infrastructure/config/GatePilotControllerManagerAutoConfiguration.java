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

import com.dt.gatepilot.controller.application.service.NodeApplyStatusAggregator;
import com.dt.gatepilot.controller.application.service.PublishedConfigReconciler;
import com.dt.gatepilot.controller.application.service.PublishedConfigStatusController;
import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;
import com.dt.gatepilot.controller.domain.port.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.domain.port.PublishedConfigStatusStore;
import com.dt.gatepilot.controller.domain.port.ReconcileResultSink;
import com.dt.gatepilot.controller.domain.port.ReleaseIntentSource;
import com.dt.gatepilot.controller.domain.port.RollbackConfigReader;
import com.dt.gatepilot.controller.infrastructure.leader.LocalControllerLeaderElector;
import com.dt.gatepilot.controller.infrastructure.scheduling.ControllerManagerDistributedLockGuard;
import com.dt.gatepilot.controller.infrastructure.scheduling.PublishedConfigStatusRefreshExecutor;
import com.dt.gatepilot.controller.infrastructure.scheduling.PublishedConfigStatusRefreshScheduler;
import com.dt.gatepilot.controller.infrastructure.scheduling.ReleaseReconcileExecutor;
import com.dt.gatepilot.controller.infrastructure.scheduling.ReleaseReconcileScheduler;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GatePilot controller-manager 自动配置。
 */
@Configuration
@ConditionalOnProperty(prefix = ControllerManagerConstants.CONFIG_PREFIX,
        name = ControllerManagerConstants.ENABLED_PROPERTY,
        havingValue = "true",
        matchIfMissing = true)
@EnableScheduling
@EnableConfigurationProperties(GatePilotControllerManagerProperties.class)
public class GatePilotControllerManagerAutoConfiguration {

    /**
     * 创建发布配置 reconcile 编排器。
     *
     * @return 发布配置 reconcile 编排器
     */
    @Bean
    @ConditionalOnMissingBean
    public PublishedConfigReconciler publishedConfigReconciler() {
        // reconciler 保持无状态，方便多副本复用
        return new PublishedConfigReconciler();
    }

    /**
     * 创建默认本地 leader 选举器。
     *
     * @param properties controller-manager 配置
     * @return leader 选举器
     */
    @Bean
    @ConditionalOnMissingBean
    public ControllerLeaderElector controllerLeaderElector(GatePilotControllerManagerProperties properties) {
        // 本地选举只用于开发和单体模式，生产会替换成分布式实现
        return new LocalControllerLeaderElector(properties.getControllerId());
    }

    /**
     * 创建发布意图 reconcile 控制器。
     *
     * @param reconciler 发布配置 reconcile 编排器
     * @param leaderElector leader 选举器
     * @param intentSource 发布意图来源
     * @param desiredStateReader 期望状态读取器
     * @param resultSink reconcile 结果写回器
     * @param rollbackConfigReader 回滚配置读取器
     * @return 发布意图 reconcile 控制器
     */
    @Bean
    @ConditionalOnBean({
            ReleaseIntentSource.class,
            GatewayDesiredStateReader.class,
            ReconcileResultSink.class,
            RollbackConfigReader.class
    })
    public ReleaseReconcileController releaseReconcileController(PublishedConfigReconciler reconciler,
                                                                 ControllerLeaderElector leaderElector,
                                                                 ReleaseIntentSource intentSource,
                                                                 GatewayDesiredStateReader desiredStateReader,
                                                                 ReconcileResultSink resultSink,
                                                                 RollbackConfigReader rollbackConfigReader) {
        // 控制器只依赖端口，不直接依赖 apiserver 实现
        return new ReleaseReconcileController(reconciler, leaderElector, intentSource, desiredStateReader, resultSink,
                rollbackConfigReader);
    }

    /**
     * 创建发布 reconcile 执行器。
     *
     * @param reconcileController 发布意图 reconcile 控制器
     * @return 发布 reconcile 执行器
     */
    @Bean
    @ConditionalOnBean(ReleaseReconcileController.class)
    public ReleaseReconcileExecutor releaseReconcileExecutor(ReleaseReconcileController reconcileController) {
        // 执行器承载 getboot 分布式锁注解，调度器只负责任务触发
        return new ReleaseReconcileExecutor(reconcileController);
    }

    /**
     * 创建发布意图定时 reconcile 调度器。
     *
     * @param properties controller-manager 配置
     * @param reconcileExecutor 发布 reconcile 执行器
     * @return 发布意图定时 reconcile 调度器
     */
    @Bean
    @ConditionalOnBean(ReleaseReconcileExecutor.class)
    public ReleaseReconcileScheduler releaseReconcileScheduler(GatePilotControllerManagerProperties properties,
                                                               ReleaseReconcileExecutor reconcileExecutor) {
        // 调度器只触发 reconcile，不承载发布逻辑
        return new ReleaseReconcileScheduler(properties, reconcileExecutor);
    }

    /**
     * 创建分布式锁启动守卫
     *
     * @param properties controller-manager 配置
     * @param beanFactory Bean 工厂
     * @return 分布式锁启动守卫
     */
    @Bean
    public ControllerManagerDistributedLockGuard controllerManagerDistributedLockGuard(
            GatePilotControllerManagerProperties properties,
            ListableBeanFactory beanFactory) {
        // 防止生产环境只留下锁注解却没有真正的锁实现
        return new ControllerManagerDistributedLockGuard(properties, beanFactory);
    }

    /**
     * 创建 PublishedConfig 状态刷新控制器
     *
     * @param leaderElector leader 选举器
     * @param statusStore 状态存取端口
     * @return PublishedConfig 状态刷新控制器
     */
    @Bean
    @ConditionalOnBean(PublishedConfigStatusStore.class)
    public PublishedConfigStatusController publishedConfigStatusController(ControllerLeaderElector leaderElector,
                                                                           PublishedConfigStatusStore statusStore) {
        // 状态刷新只聚合节点 apply 结果，不生成新发布配置
        return new PublishedConfigStatusController(leaderElector, statusStore, new NodeApplyStatusAggregator());
    }

    /**
     * 创建 PublishedConfig 状态刷新执行器
     *
     * @param statusController 状态刷新控制器
     * @return 状态刷新执行器
     */
    @Bean
    @ConditionalOnBean(PublishedConfigStatusController.class)
    public PublishedConfigStatusRefreshExecutor publishedConfigStatusRefreshExecutor(
            PublishedConfigStatusController statusController) {
        // 执行器承载 getboot 分布式锁注解
        return new PublishedConfigStatusRefreshExecutor(statusController);
    }

    /**
     * 创建 PublishedConfig 状态刷新调度器
     *
     * @param properties controller-manager 配置
     * @param refreshExecutor 状态刷新执行器
     * @return 状态刷新调度器
     */
    @Bean
    @ConditionalOnBean(PublishedConfigStatusRefreshExecutor.class)
    public PublishedConfigStatusRefreshScheduler publishedConfigStatusRefreshScheduler(
            GatePilotControllerManagerProperties properties,
            PublishedConfigStatusRefreshExecutor refreshExecutor) {
        // 状态聚合由 controller-manager 定时执行，apiserver 只负责查询
        return new PublishedConfigStatusRefreshScheduler(properties, refreshExecutor);
    }
}
