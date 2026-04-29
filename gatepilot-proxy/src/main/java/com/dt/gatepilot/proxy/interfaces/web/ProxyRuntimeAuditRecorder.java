/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.proxy.interfaces.web;

import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeMetricsSink;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRoute;
import com.dt.gatepilot.proxy.domain.runtime.ProxyAuditConstants;
import com.getboot.support.api.trace.TraceContextHolder;
import java.net.InetSocketAddress;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;

/**
 * proxy 运行审计记录器。
 */
public class ProxyRuntimeAuditRecorder {

    /**
     * 运行审计采集器。
     */
    private final RuntimeAuditSink runtimeAuditSink;

    /**
     * 运行指标采集器。
     */
    private final RuntimeMetricsSink runtimeMetricsSink;

    /**
     * 创建 proxy 运行审计记录器。
     *
     * @param runtimeAuditSink 运行审计采集器
     * @param runtimeMetricsSink 运行指标采集器
     */
    public ProxyRuntimeAuditRecorder(RuntimeAuditSink runtimeAuditSink, RuntimeMetricsSink runtimeMetricsSink) {
        this.runtimeAuditSink = runtimeAuditSink;
        this.runtimeMetricsSink = runtimeMetricsSink;
    }

    /**
     * 记录一次运行审计和指标。
     *
     * @param request WebFlux 请求
     * @param route 已命中路由
     * @param upstreamName 上游名称
     * @param upstreamUri 上游地址
     * @param status 响应状态码
     * @param trafficColor 流量颜色
     * @param methodAllowed 方法是否允许
     * @param authenticationRequired 是否需要认证
     * @param fallback 是否 fallback
     * @param outcome 执行结果
     * @param reason 结果原因
     * @param error 异常
     * @param startNanos 请求开始时间
     */
    public void record(ServerRequest request,
                       CompiledRoute route,
                       String upstreamName,
                       URI upstreamUri,
                       int status,
                       String trafficColor,
                       boolean methodAllowed,
                       boolean authenticationRequired,
                       boolean fallback,
                       String outcome,
                       String reason,
                       Throwable error,
                       long startNanos) {
        long latencyMillis = latencyMillis(startNanos);
        String routeId = route == null ? null : route.getRouteId();
        String projectName = route == null ? null : route.getProjectName();
        RuntimeAuditSink.RuntimeAuditEvent event = new RuntimeAuditSink.RuntimeAuditEvent(
                traceId(request),
                remoteAddress(request),
                request.methodName(),
                request.uri().getRawPath(),
                host(request),
                routeId,
                projectName,
                upstreamName,
                upstreamUri == null ? null : upstreamUri.toString(),
                status,
                latencyMillis,
                trafficColor,
                methodAllowed,
                authenticationRequired,
                fallback,
                outcome,
                reason,
                error == null ? null : error.getClass().getName()
        );
        emitAudit(event);
        recordMetrics(routeId, status, latencyMillis);
    }

    /**
     * 记录一次运行审计和指标。
     *
     * @param exchange WebFlux 交换上下文
     * @param route 已命中路由
     * @param upstreamName 上游名称
     * @param upstreamUri 上游地址
     * @param status 响应状态码
     * @param trafficColor 流量颜色
     * @param methodAllowed 方法是否允许
     * @param authenticationRequired 是否需要认证
     * @param fallback 是否 fallback
     * @param outcome 执行结果
     * @param reason 结果原因
     * @param error 异常
     * @param startNanos 请求开始时间
     */
    public void record(ServerWebExchange exchange,
                       CompiledRoute route,
                       String upstreamName,
                       URI upstreamUri,
                       int status,
                       String trafficColor,
                       boolean methodAllowed,
                       boolean authenticationRequired,
                       boolean fallback,
                       String outcome,
                       String reason,
                       Throwable error,
                       long startNanos) {
        long latencyMillis = latencyMillis(startNanos);
        String routeId = route == null ? null : route.getRouteId();
        String projectName = route == null ? null : route.getProjectName();
        RuntimeAuditSink.RuntimeAuditEvent event = new RuntimeAuditSink.RuntimeAuditEvent(
                traceId(exchange),
                remoteAddress(exchange),
                exchange.getRequest().getMethod().name(),
                exchange.getRequest().getURI().getRawPath(),
                host(exchange),
                routeId,
                projectName,
                upstreamName,
                upstreamUri == null ? null : upstreamUri.toString(),
                status,
                latencyMillis,
                trafficColor,
                methodAllowed,
                authenticationRequired,
                fallback,
                outcome,
                reason,
                error == null ? null : error.getClass().getName()
        );
        emitAudit(event);
        recordMetrics(routeId, status, latencyMillis);
    }

    /**
     * 发送运行审计事件。
     *
     * @param event 运行审计事件
     */
    private void emitAudit(RuntimeAuditSink.RuntimeAuditEvent event) {
        if (runtimeAuditSink == null) {
            return;
        }
        try {
            // 审计采集失败不能影响业务流量
            runtimeAuditSink.emit(event);
        } catch (RuntimeException ignored) {
            // sink 自身负责降级和告警
        }
    }

    /**
     * 记录路由运行指标。
     *
     * @param routeId 路由标识
     * @param status 响应状态码
     * @param latencyMillis 延迟
     */
    private void recordMetrics(String routeId, int status, long latencyMillis) {
        if (runtimeMetricsSink == null) {
            return;
        }
        try {
            // 指标采集失败不能影响业务流量
            runtimeMetricsSink.recordRouteRequest(routeId, status, latencyMillis);
        } catch (RuntimeException ignored) {
            // sink 自身负责降级和告警
        }
    }

    /**
     * 计算请求耗时。
     *
     * @param startNanos 请求开始时间
     * @return 请求耗时毫秒
     */
    private long latencyMillis(long startNanos) {
        // nanoTime 只用于耗时计算，不能转成业务时间
        return Math.max(0, (System.nanoTime() - startNanos) / ProxyAuditConstants.NANOS_PER_MILLISECOND);
    }

    /**
     * 解析当前 TraceId。
     *
     * @param request WebFlux 请求
     * @return TraceId
     */
    private String traceId(ServerRequest request) {
        // 优先复用 getboot 已经绑定的链路上下文
        String traceId = TraceContextHolder.getTraceId();
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        String headerTraceId = request.headers().firstHeader(ProxyAuditConstants.DEFAULT_TRACE_HEADER_NAME);
        if (StringUtils.hasText(headerTraceId)) {
            return headerTraceId;
        }
        try {
            return request.exchange().getRequest().getId();
        } catch (IllegalStateException exception) {
            // 部分单元测试请求没有绑定 exchange，审计允许缺少 traceId
            return null;
        }
    }

    /**
     * 解析当前 TraceId。
     *
     * @param exchange WebFlux 交换上下文
     * @return TraceId
     */
    private String traceId(ServerWebExchange exchange) {
        // 优先复用 getboot 已经绑定的链路上下文
        String traceId = TraceContextHolder.getTraceId();
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        String headerTraceId = exchange.getRequest().getHeaders().getFirst(ProxyAuditConstants.DEFAULT_TRACE_HEADER_NAME);
        if (StringUtils.hasText(headerTraceId)) {
            return headerTraceId;
        }
        return exchange.getRequest().getId();
    }

    /**
     * 获取客户端地址。
     *
     * @param request WebFlux 请求
     * @return 客户端地址
     */
    private String remoteAddress(ServerRequest request) {
        // 审计中的 IP 保留代理入口看到的远端地址
        return request.remoteAddress()
                .map(this::remoteAddress)
                .orElse(null);
    }

    /**
     * 获取客户端地址。
     *
     * @param exchange WebFlux 交换上下文
     * @return 客户端地址
     */
    private String remoteAddress(ServerWebExchange exchange) {
        // 审计中的 IP 保留代理入口看到的远端地址
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        if (address == null) {
            return null;
        }
        return remoteAddress(address);
    }

    /**
     * 转换客户端地址。
     *
     * @param address 客户端地址
     * @return 客户端地址文本
     */
    private String remoteAddress(InetSocketAddress address) {
        // InetAddress 为空时保留原始 host
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }

    /**
     * 获取请求域名。
     *
     * @param request WebFlux 请求
     * @return 请求域名
     */
    private String host(ServerRequest request) {
        // Host 用于把审计事件和路由命中关联起来
        return request.headers().firstHeader(HttpHeaders.HOST);
    }

    /**
     * 获取请求域名。
     *
     * @param exchange WebFlux 交换上下文
     * @return 请求域名
     */
    private String host(ServerWebExchange exchange) {
        // Host 用于把审计事件和路由命中关联起来
        return exchange.getRequest().getHeaders().getFirst(HttpHeaders.HOST);
    }
}
