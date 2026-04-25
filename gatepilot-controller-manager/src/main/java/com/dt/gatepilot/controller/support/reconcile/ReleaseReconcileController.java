package com.dt.gatepilot.controller.support.reconcile;

import com.dt.gatepilot.controller.api.ReconcileRequest;
import com.dt.gatepilot.controller.api.ReconcileResult;
import com.dt.gatepilot.controller.spi.ControllerLeaderElector;
import com.dt.gatepilot.controller.spi.GatewayDesiredStateReader;
import com.dt.gatepilot.controller.spi.ReconcileResultSink;
import com.dt.gatepilot.controller.spi.ReleaseIntentSource;
import com.dt.gatepilot.controller.support.model.GatewayDesiredState;
import com.dt.gatepilot.controller.support.model.ReleaseIntent;
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

    /**
     * 创建发布意图 reconcile 控制器。
     *
     * @param reconciler 发布配置 reconcile 编排器
     * @param leaderElector 主节点选举器
     * @param intentSource 发布意图来源
     * @param desiredStateReader 期望状态读取器
     * @param resultSink reconcile 结果写回器
     */
    public ReleaseReconcileController(PublishedConfigReconciler reconciler,
                                      ControllerLeaderElector leaderElector,
                                      ReleaseIntentSource intentSource,
                                      GatewayDesiredStateReader desiredStateReader,
                                      ReconcileResultSink resultSink) {
        this.reconciler = reconciler;
        this.leaderElector = leaderElector;
        this.intentSource = intentSource;
        this.desiredStateReader = desiredStateReader;
        this.resultSink = resultSink;
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
            GatewayDesiredState desiredState = desiredStateReader.read(intent);
            ReconcileResult result = reconciler.reconcile(toRequest(intent), desiredState);
            resultSink.save(intent, result);
            intentSource.markCompleted(intent, result);
        } catch (RuntimeException exception) {
            intentSource.markFailed(intent, exception.getClass().getSimpleName(),
                    Objects.toString(exception.getMessage(), "发布推进失败"));
        }
    }

    private ReconcileRequest toRequest(ReleaseIntent intent) {
        ReconcileRequest request = new ReconcileRequest();
        request.setNamespace(intent.getNamespace());
        request.setProjectName(intent.getProjectName());
        request.setConfigShard(intent.getConfigShard());
        request.setVersion(intent.getVersion());
        request.setSequence(intent.getSequence());
        request.setTrigger(intent.getTrigger());
        request.setRequestedBy(intent.getRequestedBy());
        request.setRequestedAt(intent.getRequestedAt());
        return request;
    }
}
