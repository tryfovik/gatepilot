package com.dt.gatepilot.controller.infrastructure.autoconfigure;

import com.dt.gatepilot.controller.api.properties.GatePilotControllerManagerProperties;
import com.dt.gatepilot.controller.infrastructure.scheduling.ReleaseReconcileScheduler;
import com.dt.gatepilot.controller.spi.ControllerLeaderElector;
import com.dt.gatepilot.controller.spi.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.spi.ReconcileResultSink;
import com.dt.gatepilot.controller.spi.ReleaseIntentSource;
import com.dt.gatepilot.controller.support.leader.LocalControllerLeaderElector;
import com.dt.gatepilot.controller.support.reconcile.PublishedConfigReconciler;
import com.dt.gatepilot.controller.support.reconcile.ReleaseReconcileController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * GatePilot controller-manager 自动配置。
 */
@Configuration
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
     * @return 发布意图 reconcile 控制器
     */
    @Bean
    @ConditionalOnBean({ReleaseIntentSource.class, GatewayDesiredStateReader.class, ReconcileResultSink.class})
    public ReleaseReconcileController releaseReconcileController(PublishedConfigReconciler reconciler,
                                                                 ControllerLeaderElector leaderElector,
                                                                 ReleaseIntentSource intentSource,
                                                                 GatewayDesiredStateReader desiredStateReader,
                                                                 ReconcileResultSink resultSink) {
        return new ReleaseReconcileController(reconciler, leaderElector, intentSource, desiredStateReader, resultSink);
    }

    /**
     * 创建发布意图定时 reconcile 调度器。
     *
     * @param properties controller-manager 配置
     * @param reconcileController 发布意图 reconcile 控制器
     * @return 发布意图定时 reconcile 调度器
     */
    @Bean
    @ConditionalOnBean(ReleaseReconcileController.class)
    public ReleaseReconcileScheduler releaseReconcileScheduler(GatePilotControllerManagerProperties properties,
                                                               ReleaseReconcileController reconcileController) {
        return new ReleaseReconcileScheduler(properties, reconcileController);
    }
}
