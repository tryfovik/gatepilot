package com.dt.gatepilot.proxy.domain.port;

import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import java.util.List;

/**
 * 上游实例发现注册表。
 */
public interface UpstreamDiscoveryRegistry {

    /**
     * 根据新运行态刷新订阅。
     *
     * @param runtime 新运行态
     */
    void refresh(CompiledProxyRuntime runtime);

    /**
     * 查询上游当前实例。
     *
     * @param upstream 已编译上游
     * @return 当前可用实例
     */
    List<CompiledUpstream.CompiledEndpoint> instances(CompiledUpstream upstream);
}
