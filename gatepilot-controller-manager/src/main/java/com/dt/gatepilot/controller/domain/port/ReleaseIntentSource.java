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
package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
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
