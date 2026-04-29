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
package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.application.command.ReconcileResult;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;

/**
 * 发布 reconcile 编排器。
 */
public class PublishedConfigReconciler {

    private final PublishedConfigAssembler assembler;

    private final NodeApplyStatusAggregator statusAggregator;

    private final ReleaseEventFactory eventFactory;

    /**
     * 创建发布 reconcile 编排器。
     */
    public PublishedConfigReconciler() {
        this(new PublishedConfigAssembler(), new NodeApplyStatusAggregator(), new ReleaseEventFactory());
    }

    /**
     * 创建发布 reconcile 编排器。
     *
     * @param assembler PublishedConfig 组装器
     * @param statusAggregator 状态聚合器
     * @param eventFactory 事件工厂
     */
    public PublishedConfigReconciler(PublishedConfigAssembler assembler,
                                     NodeApplyStatusAggregator statusAggregator,
                                     ReleaseEventFactory eventFactory) {
        this.assembler = assembler;
        this.statusAggregator = statusAggregator;
        this.eventFactory = eventFactory;
    }

    /**
     * 执行 reconcile。
     *
     * @param request reconcile 请求
     * @param desiredState 期望状态
     * @return reconcile 结果
     */
    public ReconcileResult reconcile(ReconcileRequest request, GatewayDesiredState desiredState) {
        PublishedConfig publishedConfig = assembler.assemble(request, desiredState);
        statusAggregator.aggregate(publishedConfig, desiredState.getTargetNodes());
        GatewayEvent event = eventFactory.publishedConfigGenerated(publishedConfig);
        ReconcileResult result = new ReconcileResult();
        result.setPublishedConfig(publishedConfig);
        result.setEvent(event);
        return result;
    }

    /**
     * 执行回滚 reconcile。
     *
     * @param request reconcile 请求
     * @param sourceConfig 回滚目标快照中的已发布配置
     * @return reconcile 结果
     */
    public ReconcileResult rollback(ReconcileRequest request, PublishedConfig sourceConfig) {
        PublishedConfig publishedConfig = assembler.assembleRollback(request, sourceConfig);
        GatewayEvent event = eventFactory.rollbackConfigGenerated(publishedConfig, sourceConfig);
        ReconcileResult result = new ReconcileResult();
        // 回滚结果仍写入 PublishedConfig，后续 agent/proxy 无需识别特殊链路
        result.setPublishedConfig(publishedConfig);
        result.setEvent(event);
        return result;
    }
}
