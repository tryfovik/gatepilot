package com.dt.gatepilot.apiserver.application.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 路由诊断请求。
 */
@Data
public class RouteDiagnosticsRequest {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 发布版本，为空时使用最新版本。
     */
    private String version;

    /**
     * 配置分片。
     */
    private String configShard;

    /**
     * HTTP 方法。
     */
    private String method;

    /**
     * 请求 Host。
     */
    private String host;

    /**
     * 请求路径，可携带 query。
     */
    private String path;

    /**
     * 请求头。
     */
    private Map<String, List<String>> headers = new LinkedHashMap<>();

    /**
     * Query 参数。
     */
    private Map<String, List<String>> query = new LinkedHashMap<>();

    /**
     * Cookie 参数。
     */
    private Map<String, List<String>> cookies = new LinkedHashMap<>();

    /**
     * 远端地址。
     */
    private String remoteAddress;
}
