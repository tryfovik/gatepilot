package com.dt.gatepilot.agent.spi;

import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import java.util.function.Consumer;

/**
 * 已发布配置订阅扩展点，后续可接入 long polling、SSE 或 WebSocket。
 */
public interface PublishedConfigSubscriber {

    /**
     * 订阅配置变化。
     *
     * @param listener 配置监听器
     */
    void subscribe(Consumer<PublishedConfig> listener);
}
