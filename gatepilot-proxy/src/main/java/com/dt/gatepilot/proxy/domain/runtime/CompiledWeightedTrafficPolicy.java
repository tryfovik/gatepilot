package com.dt.gatepilot.proxy.domain.runtime;

import java.util.List;

/**
 * proxy 预编译权重染色策略。
 *
 * @param policyName 策略名称
 * @param weightHashHeaders 权重 hash 请求头
 * @param splits 权重分组
 */
public record CompiledWeightedTrafficPolicy(String policyName,
                                            List<String> weightHashHeaders,
                                            List<CompiledTrafficSplit> splits) {
}
