package com.dt.gatepilot.controller.support.leader;

import com.dt.gatepilot.controller.spi.ControllerLeaderElector;

/**
 * 本地单实例 leader 选举器。
 */
public class LocalControllerLeaderElector implements ControllerLeaderElector {

    private final String controllerId;

    /**
     * 创建本地 leader 选举器。
     *
     * @param controllerId controller-manager 实例标识
     */
    public LocalControllerLeaderElector(String controllerId) {
        this.controllerId = controllerId;
    }

    @Override
    public boolean isLeader() {
        return true;
    }

    @Override
    public String currentLeaderId() {
        return controllerId;
    }
}
