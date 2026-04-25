package com.dt.gatepilot.agent.spi;

import com.dt.gatepilot.agent.api.AgentApplyResult;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;

/**
 * agent 调用本机 proxy 应用配置的客户端扩展点。
 */
public interface ProxyApplyClient {

    /**
     * 应用配置。
     *
     * @param config 已发布配置
     * @return 应用结果
     */
    AgentApplyResult apply(PublishedConfig config);
}
