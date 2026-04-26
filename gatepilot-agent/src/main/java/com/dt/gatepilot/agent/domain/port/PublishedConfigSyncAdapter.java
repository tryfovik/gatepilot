package com.dt.gatepilot.agent.domain.port;

import com.dt.gatepilot.agent.application.dto.AgentConfigCursor;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * 已发布配置同步适配端口。
 */
public interface PublishedConfigSyncAdapter {

    /**
     * 按节点游标同步已发布配置。
     *
     * @param cursor 本地配置游标
     * @return 最新配置
     */
    Optional<PublishedConfig> sync(AgentConfigCursor cursor);
}
