package com.dt.gatepilot.proxy.spi;

/**
 * proxy 运行审计采集扩展点，只负责采集和转发，不提供管理查询。
 */
public interface RuntimeAuditSink {

    /**
     * 记录运行审计事件。
     *
     * @param event 审计事件
     */
    void emit(RuntimeAuditEvent event);

    /**
     * 运行审计事件。
     *
     * @param traceId TraceId
     * @param routeId 路由标识
     * @param upstreamName 上游名称
     * @param status 响应状态码
     * @param latencyMillis 延迟
     */
    record RuntimeAuditEvent(String traceId,
                             String routeId,
                             String upstreamName,
                             int status,
                             long latencyMillis) {
    }
}
