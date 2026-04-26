package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;
import com.dt.gatepilot.controller.domain.port.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.domain.port.ReconcileResultSink;
import com.dt.gatepilot.controller.domain.port.ReleaseIntentSource;
import com.dt.gatepilot.controller.domain.port.RollbackConfigReader;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 发布 reconcile 控制器多副本语义测试
 */
class ReleaseReconcileControllerTest {

    @Test
    void shouldSkipPendingListWhenCurrentNodeIsStandby() {
        StubIntentSource intentSource = new StubIntentSource(List.of(intent("release-1")));
        CountingReconciler reconciler = new CountingReconciler();
        ReleaseReconcileController controller = controller(reconciler, standbyLeader(), intentSource);

        int processed = controller.reconcileBatch(10);

        assertThat(processed).isZero();
        assertThat(intentSource.listCount).isZero();
        assertThat(reconciler.reconcileCount).isZero();
    }

    @Test
    void shouldSkipIntentWhenClaimFailed() {
        StubIntentSource intentSource = new StubIntentSource(List.of(intent("release-1")));
        intentSource.claimResult = false;
        CountingReconciler reconciler = new CountingReconciler();
        ReleaseReconcileController controller = controller(reconciler, activeLeader("controller-a"), intentSource);

        int processed = controller.reconcileBatch(10);

        assertThat(processed).isZero();
        assertThat(intentSource.claimedControllerIds).containsExactly("controller-a");
        assertThat(intentSource.completedCount).isZero();
        assertThat(reconciler.reconcileCount).isZero();
    }

    @Test
    void shouldProcessOnlyClaimedIntent() {
        StubIntentSource intentSource = new StubIntentSource(List.of(intent("release-1"), intent("release-2")));
        intentSource.claimResults.add(true);
        intentSource.claimResults.add(false);
        CountingReconciler reconciler = new CountingReconciler();
        ReleaseReconcileController controller = controller(reconciler, activeLeader("controller-a"), intentSource);

        int processed = controller.reconcileBatch(10);

        assertThat(processed).isEqualTo(1);
        assertThat(intentSource.claimedControllerIds).containsExactly("controller-a", "controller-a");
        assertThat(intentSource.completedCount).isEqualTo(1);
        assertThat(reconciler.reconcileCount).isEqualTo(1);
    }

    private ReleaseReconcileController controller(CountingReconciler reconciler,
                                                 ControllerLeaderElector leaderElector,
                                                 StubIntentSource intentSource) {
        GatewayDesiredStateReader desiredStateReader = ignored -> new GatewayDesiredState();
        ReconcileResultSink resultSink = (intent, result) -> {
            // 当前测试只验证多副本推进语义
        };
        RollbackConfigReader rollbackConfigReader = ignored -> new PublishedConfig();
        return new ReleaseReconcileController(reconciler, leaderElector, intentSource, desiredStateReader,
                resultSink, rollbackConfigReader);
    }

    private ControllerLeaderElector activeLeader(String controllerId) {
        return new ControllerLeaderElector() {
            @Override
            public boolean isLeader() {
                return true;
            }

            @Override
            public String currentLeaderId() {
                return controllerId;
            }
        };
    }

    private ControllerLeaderElector standbyLeader() {
        return new ControllerLeaderElector() {
            @Override
            public boolean isLeader() {
                return false;
            }

            @Override
            public String currentLeaderId() {
                return "standby-controller";
            }
        };
    }

    private ReleaseIntent intent(String releaseId) {
        ReleaseIntent intent = new ReleaseIntent();
        intent.setReleaseId(releaseId);
        intent.setNamespace("default");
        intent.setProjectName("game");
        intent.setVersion(releaseId);
        return intent;
    }

    private static class CountingReconciler extends PublishedConfigReconciler {

        private int reconcileCount;

        @Override
        public ReconcileResult reconcile(ReconcileRequest request, GatewayDesiredState desiredState) {
            reconcileCount++;
            ReconcileResult result = new ReconcileResult();
            result.setPublishedConfig(new PublishedConfig());
            return result;
        }
    }

    private static class StubIntentSource implements ReleaseIntentSource {

        private final List<ReleaseIntent> intents;

        private final List<Boolean> claimResults = new ArrayList<>();

        private final List<String> claimedControllerIds = new ArrayList<>();

        private boolean claimResult = true;

        private int listCount;

        private int completedCount;

        StubIntentSource(List<ReleaseIntent> intents) {
            this.intents = intents;
        }

        @Override
        public List<ReleaseIntent> listPending(int limit) {
            listCount++;
            return intents.stream().limit(limit).toList();
        }

        @Override
        public boolean claim(ReleaseIntent intent, String controllerId) {
            claimedControllerIds.add(controllerId);
            if (!claimResults.isEmpty()) {
                return claimResults.remove(0);
            }
            return claimResult;
        }

        @Override
        public void markCompleted(ReleaseIntent intent, ReconcileResult result) {
            completedCount++;
        }

        @Override
        public void markFailed(ReleaseIntent intent, String reason, String message) {
            // 当前测试只覆盖正常 claim 语义
        }
    }
}
