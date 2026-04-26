package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;
import com.dt.gatepilot.controller.domain.port.PublishedConfigStatusStore;
import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.List;

/**
 * PublishedConfig 应用状态刷新控制器
 */
public class PublishedConfigStatusController {

    private final ControllerLeaderElector leaderElector;

    private final PublishedConfigStatusStore statusStore;

    private final NodeApplyStatusAggregator statusAggregator;

    /**
     * 创建 PublishedConfig 应用状态刷新控制器
     *
     * @param leaderElector leader 选举器
     * @param statusStore 状态存取端口
     * @param statusAggregator 节点应用状态聚合器
     */
    public PublishedConfigStatusController(ControllerLeaderElector leaderElector,
                                           PublishedConfigStatusStore statusStore,
                                           NodeApplyStatusAggregator statusAggregator) {
        this.leaderElector = leaderElector;
        this.statusStore = statusStore;
        this.statusAggregator = statusAggregator;
    }

    /**
     * 刷新一批 PublishedConfig 应用状态
     *
     * @param limit 最大刷新数量
     * @return 已刷新数量
     */
    public int refreshBatch(int limit) {
        if (!leaderElector.isLeader()) {
            return 0;
        }
        int refreshed = 0;
        for (PublishedConfig publishedConfig : statusStore.listPublishedConfigs(limit)) {
            List<GatewayNode> targetNodes = statusStore.listTargetNodes(publishedConfig);
            statusAggregator.aggregate(publishedConfig, targetNodes);
            statusStore.saveStatus(publishedConfig);
            refreshed++;
        }
        return refreshed;
    }
}
