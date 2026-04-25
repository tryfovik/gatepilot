package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;

/**
 * 回滚配置读取端口。
 */
public interface RollbackConfigReader {

    /**
     * 读取回滚目标版本的已发布配置。
     *
     * @param intent 发布意图
     * @return 已发布配置
     */
    PublishedConfig readRollbackConfig(ReleaseIntent intent);
}
