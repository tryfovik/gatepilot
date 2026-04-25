package com.dt.gatepilot.agent.spi;

import com.dt.gatepilot.agent.api.AgentApplyResult;
import com.dt.gatepilot.agent.api.AgentConfigCursor;
import com.dt.gatepilot.agent.api.AgentHeartbeatSnapshot;
import com.dt.gatepilot.agent.api.AgentNodeProfile;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * agent 访问控制面的客户端扩展点。
 */
public interface AgentControlPlaneClient {

    /**
     * 注册节点。
     *
     * @param profile 节点身份信息
     */
    void register(AgentNodeProfile profile);

    /**
     * 发送心跳。
     *
     * @param heartbeat 心跳快照
     */
    void heartbeat(AgentHeartbeatSnapshot heartbeat);

    /**
     * 拉取已发布配置。
     *
     * @param cursor 本地配置游标
     * @return 最新配置
     */
    Optional<PublishedConfig> pullConfig(AgentConfigCursor cursor);

    /**
     * 上报配置应用结果。
     *
     * @param result 应用结果
     */
    void reportApplyResult(AgentApplyResult result);
}
