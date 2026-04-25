package com.dt.gatepilot.apiserver.api.response;

import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import lombok.Data;

/**
 * agent 拉取已发布配置响应。
 */
@Data
public class AgentConfigPullResponse {

    /**
     * 是否存在新配置。
     */
    private boolean changed;

    /**
     * 最新已发布配置。
     */
    private PublishedConfig publishedConfig;

    /**
     * 控制面提示信息。
     */
    private String message;
}
