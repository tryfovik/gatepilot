package com.dt.gatepilot.demo.application.service;

import com.dt.gatepilot.demo.application.dto.DemoEchoResponse;
import com.dt.gatepilot.demo.infrastructure.config.DemoUpstreamProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

/**
 * 示例上游回显服务
 */
@Service
public class DemoEchoService {

    /**
     * 示例配置
     */
    private final DemoUpstreamProperties properties;

    /**
     * 时间来源
     */
    private final Clock clock;

    /**
     * 创建示例上游回显服务
     *
     * @param properties 示例配置
     */
    @Autowired
    public DemoEchoService(DemoUpstreamProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * 创建示例上游回显服务
     *
     * @param properties 示例配置
     * @param clock 时间来源
     */
    DemoEchoService(DemoUpstreamProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 构建回显响应
     *
     * @param request 上游收到的请求
     * @return 回显响应
     */
    public DemoEchoResponse echo(ServerHttpRequest request) {
        DemoEchoResponse response = new DemoEchoResponse();
        response.setServiceName(properties.getServiceName());
        response.setVersion(properties.getVersion());
        response.setColor(properties.getColor());
        response.setInstanceId(properties.getInstanceId());
        response.setZone(properties.getZone());
        response.setMethod(request.getMethod().name());
        response.setPath(request.getURI().getRawPath());
        response.setQuery(query(request));
        response.setHeaders(headers(request.getHeaders()));
        response.setTimestamp(Instant.now(clock));
        response.setMessage(properties.getMessage());
        return response;
    }

    /**
     * 提取原始查询串
     *
     * @param request 上游收到的请求
     * @return 查询串
     */
    private String query(ServerHttpRequest request) {
        String rawQuery = request.getURI().getRawQuery();
        // JSON 里保留空串，比 null 更直观看出没有 query
        return rawQuery == null ? DemoEchoConstants.EMPTY_QUERY : rawQuery;
    }

    /**
     * 提取关键请求头
     *
     * @param headers 请求头
     * @return 关键请求头
     */
    private java.util.Map<String, String> headers(HttpHeaders headers) {
        java.util.Map<String, String> selectedHeaders = new java.util.LinkedHashMap<>();
        for (String headerName : DemoEchoConstants.ECHO_HEADER_NAMES) {
            String value = headers.getFirst(headerName);
            if (value != null) {
                selectedHeaders.put(headerName, value);
            }
        }
        return selectedHeaders;
    }
}
