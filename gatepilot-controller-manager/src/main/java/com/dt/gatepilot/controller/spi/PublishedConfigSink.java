package com.dt.gatepilot.controller.spi;

import com.dt.gatepilot.api.resource.publish.PublishedConfig;

/**
 * 已发布配置写入扩展点。
 */
public interface PublishedConfigSink {

    /**
     * 保存已发布配置。
     *
     * @param publishedConfig 已发布配置
     */
    void save(PublishedConfig publishedConfig);
}
