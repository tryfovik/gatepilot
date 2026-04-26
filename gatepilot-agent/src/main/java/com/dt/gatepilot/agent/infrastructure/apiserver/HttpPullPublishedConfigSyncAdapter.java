package com.dt.gatepilot.agent.infrastructure.apiserver;

import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.agent.domain.port.AgentControlPlaneClient;
import com.dt.gatepilot.agent.domain.port.PublishedConfigSyncAdapter;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * 基于 apiserver HTTP pull 的配置同步适配器。
 */
public class HttpPullPublishedConfigSyncAdapter implements PublishedConfigSyncAdapter {

    /**
     * 控制面客户端。
     */
    private final AgentControlPlaneClient controlPlaneClient;

    /**
     * 创建 HTTP pull 配置同步适配器。
     *
     * @param controlPlaneClient 控制面客户端
     */
    public HttpPullPublishedConfigSyncAdapter(AgentControlPlaneClient controlPlaneClient) {
        this.controlPlaneClient = controlPlaneClient;
    }

    /**
     * 按节点游标同步已发布配置。
     *
     * @param cursor 本地配置游标
     * @return 最新配置
     */
    @Override
    public Optional<PublishedConfig> sync(AgentConfigCursor cursor) {
        // 当前主路径是 HTTP pull，后续 Nacos watch 可替换这个端口
        return controlPlaneClient.pullConfig(cursor);
    }
}
