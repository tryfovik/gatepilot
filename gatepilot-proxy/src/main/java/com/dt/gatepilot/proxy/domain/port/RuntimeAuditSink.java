package com.dt.gatepilot.proxy.domain.port;

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
     * @param clientIp 客户端 IP
     * @param method HTTP 方法
     * @param path 请求路径
     * @param host 请求域名
     * @param routeId 路由标识
     * @param projectName 项目名称
     * @param upstreamName 上游名称
     * @param upstreamUri 上游地址
     * @param status 响应状态码
     * @param latencyMillis 延迟
     * @param trafficColor 流量颜色
     * @param methodAllowed 方法是否允许
     * @param authenticationRequired 是否需要认证
     * @param fallback 是否 fallback
     * @param outcome 执行结果
     * @param reason 结果原因
     * @param error 异常类型
     */
    record RuntimeAuditEvent(String traceId,
                             String clientIp,
                             String method,
                             String path,
                             String host,
                             String routeId,
                             String projectName,
                             String upstreamName,
                             String upstreamUri,
                             int status,
                             long latencyMillis,
                             String trafficColor,
                             boolean methodAllowed,
                             boolean authenticationRequired,
                             boolean fallback,
                             String outcome,
                             String reason,
                             String error) {
    }
}
