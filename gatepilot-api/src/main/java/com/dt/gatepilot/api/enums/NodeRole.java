package com.dt.gatepilot.api.enums;

/**
 * GatePilot 节点角色。
 */
public enum NodeRole {

    /**
     * 只运行 agent。
     */
    AGENT,

    /**
     * 只运行 proxy。
     */
    PROXY,

    /**
     * agent 与 proxy 同进程或同节点运行。
     */
    COMBINED
}
