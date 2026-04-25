package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.controller.domain.model.ReleaseIntent;

/**
 * 期望状态读取端口。
 */
public interface GatewayDesiredStateReader {

    /**
     * 读取发布意图对应的控制面期望状态。
     *
     * @param intent 发布意图
     * @return 期望状态
     */
    GatewayDesiredState read(ReleaseIntent intent);
}
