package com.dt.gatepilot.demo.application.dto;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * 示例上游回显响应
 */
@Data
public class DemoEchoResponse {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 实例版本
     */
    private String version;

    /**
     * 流量颜色
     */
    private String color;

    /**
     * 实例标识
     */
    private String instanceId;

    /**
     * 部署区域
     */
    private String zone;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 上游收到的路径
     */
    private String path;

    /**
     * 查询串
     */
    private String query;

    /**
     * 关键请求头
     */
    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * 响应时间
     */
    private Instant timestamp;

    /**
     * 示例提示
     */
    private String message;
}
