package com.dt.gatepilot.proxy.domain.port;

/**
 * proxy 运行指标采集扩展点。
 */
public interface RuntimeMetricsSink {

    /**
     * 记录路由请求指标。
     *
     * @param routeId 路由标识
     * @param status 响应状态码
     * @param latencyMillis 延迟
     */
    void recordRouteRequest(String routeId, int status, long latencyMillis);
}
