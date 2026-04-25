package com.dt.gatepilot.controller.spi;

import com.dt.gatepilot.controller.api.ReconcileResult;
import com.dt.gatepilot.controller.support.model.ReleaseIntent;
import java.util.List;

/**
 * 发布意图来源，由 apiserver 或 apiserver HTTP 适配器提供。
 */
public interface ReleaseIntentSource {

    /**
     * 查询等待推进的发布意图。
     *
     * @param limit 最大返回数量
     * @return 发布意图列表
     */
    List<ReleaseIntent> listPending(int limit);

    /**
     * 领取发布意图，避免多副本重复推进。
     *
     * @param intent 发布意图
     * @param controllerId controller-manager 实例标识
     * @return 是否领取成功
     */
    boolean claim(ReleaseIntent intent, String controllerId);

    /**
     * 标记发布意图推进完成。
     *
     * @param intent 发布意图
     * @param result reconcile 结果
     */
    void markCompleted(ReleaseIntent intent, ReconcileResult result);

    /**
     * 标记发布意图推进失败。
     *
     * @param intent 发布意图
     * @param reason 失败原因码
     * @param message 失败说明
     */
    void markFailed(ReleaseIntent intent, String reason, String message);
}
