package com.dt.gatepilot.proxy.domain.port;

import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;

/**
 * 运行态治理规则发布端口。
 */
public interface RuntimeGovernanceRulePublisher {

    /**
     * 发布运行态治理规则。
     *
     * @param runtime 已编译运行态
     */
    void publish(CompiledProxyRuntime runtime);
}
