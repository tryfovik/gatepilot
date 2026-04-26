package com.dt.gatepilot.agent.domain.port;

import com.dt.gatepilot.agent.application.dto.AgentHeartbeatSnapshot;

/**
 * agent 读取本机 proxy 运行状态的端口。
 */
public interface ProxyRuntimeStatusReader {

    /**
     * 填充 proxy 运行状态。
     *
     * @param snapshot 心跳快照
     */
    void fill(AgentHeartbeatSnapshot snapshot);
}
