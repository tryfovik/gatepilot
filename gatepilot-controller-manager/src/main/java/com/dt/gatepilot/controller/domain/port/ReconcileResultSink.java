package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;

/**
 * reconcile 结果写回端口。
 */
public interface ReconcileResultSink {

    /**
     * 写回 reconcile 结果。
     *
     * @param intent 发布意图
     * @param result reconcile 结果
     */
    void save(ReleaseIntent intent, ReconcileResult result);
}
