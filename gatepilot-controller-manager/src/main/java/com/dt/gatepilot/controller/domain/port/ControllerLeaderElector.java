package com.dt.gatepilot.controller.domain.port;

/**
 * controller-manager 主节点选举扩展点。
 */
public interface ControllerLeaderElector {

    /**
     * 当前实例是否为 active leader。
     *
     * @return 是否为 leader
     */
    boolean isLeader();

    /**
     * 当前 leader 标识。
     *
     * @return leader 标识
     */
    String currentLeaderId();
}
