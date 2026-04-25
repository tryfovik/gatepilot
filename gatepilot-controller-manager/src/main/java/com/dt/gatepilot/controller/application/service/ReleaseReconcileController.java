package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;
import com.dt.gatepilot.controller.domain.port.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.domain.port.ReconcileResultSink;
import com.dt.gatepilot.controller.domain.port.ReleaseIntentSource;
import com.dt.gatepilot.controller.domain.port.RollbackConfigReader;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.domain.resource.event.GatewayEventConstants;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.List;
import java.util.Objects;

/**
 * 发布意图 reconcile 控制器。
 */
public class ReleaseReconcileController {

    private final PublishedConfigReconciler reconciler;

    private final ControllerLeaderElector leaderElector;

    private final ReleaseIntentSource intentSource;

    private final GatewayDesiredStateReader desiredStateReader;

    private final ReconcileResultSink resultSink;

    private final RollbackConfigReader rollbackConfigReader;

    /**
     * 创建发布意图 reconcile 控制器。
     *
     * @param reconciler 发布配置 reconcile 编排器
     * @param leaderElector 主节点选举器
     * @param intentSource 发布意图来源
     * @param desiredStateReader 期望状态读取器
     * @param resultSink reconcile 结果写回器
     * @param rollbackConfigReader 回滚配置读取器
     */
    public ReleaseReconcileController(PublishedConfigReconciler reconciler,
                                      ControllerLeaderElector leaderElector,
                                      ReleaseIntentSource intentSource,
                                      GatewayDesiredStateReader desiredStateReader,
                                      ReconcileResultSink resultSink,
                                      RollbackConfigReader rollbackConfigReader) {
        this.reconciler = reconciler;
        this.leaderElector = leaderElector;
        this.intentSource = intentSource;
        this.desiredStateReader = desiredStateReader;
        this.resultSink = resultSink;
        this.rollbackConfigReader = rollbackConfigReader;
    }

    /**
     * 推进一批等待发布的意图。
     *
     * @param limit 最大推进数量
     * @return 已处理数量
     */
    public int reconcileBatch(int limit) {
        if (!leaderElector.isLeader()) {
            return 0;
        }
        List<ReleaseIntent> intents = intentSource.listPending(limit);
        int processed = 0;
        for (ReleaseIntent intent : intents) {
            if (!intentSource.claim(intent, leaderElector.currentLeaderId())) {
                continue;
            }
            reconcileClaimedIntent(intent);
            processed++;
        }
        return processed;
    }

    private void reconcileClaimedIntent(ReleaseIntent intent) {
        try {
            ReconcileResult result = reconcile(intent);
            resultSink.save(intent, result);
            intentSource.markCompleted(intent, result);
        } catch (RuntimeException exception) {
            intentSource.markFailed(intent, exception.getClass().getSimpleName(),
                    Objects.toString(exception.getMessage(), "发布推进失败"));
        }
    }

    private ReconcileResult reconcile(ReleaseIntent intent) {
        if (GatewayEventConstants.TRIGGER_ROLLBACK.equals(intent.getTrigger())) {
            // 回滚使用历史 PublishedConfig 快照，不重新读取当前草稿资源
            PublishedConfig sourceConfig = rollbackConfigReader.readRollbackConfig(intent);
            return reconciler.rollback(toRequest(intent), sourceConfig);
        }
        GatewayDesiredState desiredState = desiredStateReader.read(intent);
        return reconciler.reconcile(toRequest(intent), desiredState);
    }

    private ReconcileRequest toRequest(ReleaseIntent intent) {
        ReconcileRequest request = new ReconcileRequest();
        request.setNamespace(intent.getNamespace());
        request.setProjectName(intent.getProjectName());
        request.setConfigShard(intent.getConfigShard());
        request.setVersion(intent.getVersion());
        request.setTargetVersion(intent.getTargetVersion());
        request.setSequence(intent.getSequence());
        request.setTrigger(intent.getTrigger());
        request.setRequestedBy(intent.getRequestedBy());
        request.setRequestedAt(intent.getRequestedAt());
        return request;
    }
}
