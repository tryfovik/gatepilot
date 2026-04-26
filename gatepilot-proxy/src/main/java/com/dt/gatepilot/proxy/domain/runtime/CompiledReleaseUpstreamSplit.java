package com.dt.gatepilot.proxy.domain.runtime;

/**
 * proxy 预编译发布上游分流。
 *
 * @param target 目标分组
 * @param color 流量颜色
 * @param upstreamName 上游名称
 */
public record CompiledReleaseUpstreamSplit(String target, String color, String upstreamName) {

    /**
     * 判断流量颜色是否命中分流。
     *
     * @param trafficColor 流量颜色
     * @return 是否命中
     */
    public boolean matches(String trafficColor) {
        return trafficColor.equals(color) || trafficColor.equals(target);
    }
}
