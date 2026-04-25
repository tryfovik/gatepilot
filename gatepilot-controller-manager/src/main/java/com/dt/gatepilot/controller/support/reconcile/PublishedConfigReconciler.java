package com.dt.gatepilot.controller.support.reconcile;

import com.dt.gatepilot.api.resource.event.GatewayEvent;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import com.dt.gatepilot.controller.api.ReconcileRequest;
import com.dt.gatepilot.controller.api.ReconcileResult;
import com.dt.gatepilot.controller.support.model.GatewayDesiredState;

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
}
