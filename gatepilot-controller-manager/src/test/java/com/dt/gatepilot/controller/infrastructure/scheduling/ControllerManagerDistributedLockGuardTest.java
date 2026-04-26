package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.application.service.PublishedConfigReconciler;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;
import com.dt.gatepilot.controller.domain.port.ReleaseIntentSource;
import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.dt.gatepilot.controller.infrastructure.config.GatePilotControllerManagerProperties;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.getboot.lock.api.properties.LockProperties;
import com.getboot.lock.infrastructure.database.jdbc.aspect.JdbcDistributedLockAspect;
import com.getboot.lock.infrastructure.database.jdbc.support.JdbcDistributedLockRepository;
import com.getboot.lock.support.DefaultDistributedLockAcquireFailureHandler;
import com.getboot.lock.support.SpelDistributedLockKeyResolver;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * controller-manager 分布式锁启动守卫测试
 */
class ControllerManagerDistributedLockGuardTest {

    @Test
    void shouldFailWhenLockRequiredButNoGetbootLockAspect() {
        try (GenericApplicationContext context = contextWithReleaseExecutor()) {
            context.refresh();
            GatePilotControllerManagerProperties properties = new GatePilotControllerManagerProperties();
            ControllerManagerDistributedLockGuard guard =
                    new ControllerManagerDistributedLockGuard(properties, context);

            assertThatThrownBy(guard::afterSingletonsInstantiated)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(ControllerManagerConstants.MESSAGE_LOCK_IMPLEMENTATION_MISSING);
        }
    }

    @Test
    void shouldSkipGuardWhenLockNotRequired() {
        try (GenericApplicationContext context = contextWithReleaseExecutor()) {
            context.refresh();
            GatePilotControllerManagerProperties properties = new GatePilotControllerManagerProperties();
            properties.setDistributedLockRequired(false);
            ControllerManagerDistributedLockGuard guard =
                    new ControllerManagerDistributedLockGuard(properties, context);

            assertThatCode(guard::afterSingletonsInstantiated).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldPassWhenGetbootLockAspectExists() {
        try (GenericApplicationContext context = contextWithReleaseExecutor()) {
            context.registerBean(JdbcDistributedLockAspect.class, this::jdbcDistributedLockAspect);
            context.refresh();
            GatePilotControllerManagerProperties properties = new GatePilotControllerManagerProperties();
            ControllerManagerDistributedLockGuard guard =
                    new ControllerManagerDistributedLockGuard(properties, context);

            assertThatCode(guard::afterSingletonsInstantiated).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldSkipGuardWhenNoSchedulingExecutorExists() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.refresh();
            GatePilotControllerManagerProperties properties = new GatePilotControllerManagerProperties();
            ControllerManagerDistributedLockGuard guard =
                    new ControllerManagerDistributedLockGuard(properties, context);

            assertThatCode(guard::afterSingletonsInstantiated).doesNotThrowAnyException();
        }
    }

    private GenericApplicationContext contextWithReleaseExecutor() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(ReleaseReconcileExecutor.class,
                () -> new ReleaseReconcileExecutor(releaseReconcileController()));
        return context;
    }

    private ReleaseReconcileController releaseReconcileController() {
        return new ReleaseReconcileController(
                new PublishedConfigReconciler(),
                new AlwaysLeaderElector(),
                new EmptyIntentSource(),
                ignored -> new GatewayDesiredState(),
                (intent, result) -> {
                    // 当前测试只关心锁守卫，不触发真实 reconcile
                },
                ignored -> new PublishedConfig()
        );
    }

    private JdbcDistributedLockAspect jdbcDistributedLockAspect() {
        return new JdbcDistributedLockAspect(
                new JdbcDistributedLockRepository(new JdbcTemplate(), "distributed_lock"),
                new SpelDistributedLockKeyResolver(),
                new DefaultDistributedLockAcquireFailureHandler(),
                new LockProperties()
        );
    }

    private static class AlwaysLeaderElector implements ControllerLeaderElector {

        @Override
        public boolean isLeader() {
            return true;
        }

        @Override
        public String currentLeaderId() {
            return "controller-a";
        }
    }

    private static class EmptyIntentSource implements ReleaseIntentSource {

        @Override
        public List<ReleaseIntent> listPending(int limit) {
            return List.of();
        }

        @Override
        public boolean claim(ReleaseIntent intent, String controllerId) {
            return false;
        }

        @Override
        public void markCompleted(ReleaseIntent intent, ReconcileResult result) {
            // 空实现避免测试触达真实存储
        }

        @Override
        public void markFailed(ReleaseIntent intent, String reason, String message) {
            // 空实现避免测试触达真实存储
        }
    }
}
