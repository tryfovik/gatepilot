package com.dt.gatepilot.agent.domain.port;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * agent 本地配置存储扩展点。
 */
public interface LocalConfigStore {

    /**
     * 保存 staged 配置。
     *
     * @param config 已发布配置
     */
    void saveStaged(PublishedConfig config);

    /**
     * 查询 staged 配置。
     *
     * @return staged 配置
     */
    Optional<PublishedConfig> loadStaged();

    /**
     * 将指定配置晋升为 last-good。
     *
     * @param config 已发布配置
     */
    void promoteLastGood(PublishedConfig config);

    /**
     * 查询 last-good 配置。
     *
     * @return last-good 配置
     */
    Optional<PublishedConfig> loadLastGood();
}
