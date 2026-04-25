package com.dt.gatepilot.controller.spi;

import com.dt.gatepilot.controller.api.ReconcileResult;
import com.dt.gatepilot.controller.support.model.ReleaseIntent;

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
