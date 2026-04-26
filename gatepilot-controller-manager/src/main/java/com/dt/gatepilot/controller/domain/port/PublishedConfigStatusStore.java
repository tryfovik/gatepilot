package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.List;

/**
 * PublishedConfig 状态存取端口
 */
public interface PublishedConfigStatusStore {

    /**
     * 查询需要刷新状态的 PublishedConfig
     *
     * @param limit 最大返回数量
     * @return PublishedConfig 列表
     */
    List<PublishedConfig> listPublishedConfigs(int limit);

    /**
     * 查询 PublishedConfig 的目标节点
     *
     * @param publishedConfig 已发布配置
     * @return 目标节点列表
     */
    List<GatewayNode> listTargetNodes(PublishedConfig publishedConfig);

    /**
     * 保存 PublishedConfig 状态
     *
     * @param publishedConfig 已发布配置
     */
    void saveStatus(PublishedConfig publishedConfig);
}
