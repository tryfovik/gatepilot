package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 预编译权重染色分组。
 *
 * @param color 流量颜色
 * @param weight 权重
 */
public record CompiledTrafficSplit(String color, int weight) {
}
