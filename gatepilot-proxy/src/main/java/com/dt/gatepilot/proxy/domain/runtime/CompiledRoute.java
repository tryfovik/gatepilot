package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * proxy 预编译路由，业务请求热路径只读取这个不可变快照。
 */
@Data
public class CompiledRoute {

    /**
     * 路由标识。
     */
    private String routeId;

    /**
     * 入口协议。
     */
    private List<Protocol> protocols = new ArrayList<>();

    /**
     * 匹配域名。
     */
    private List<String> hosts = new ArrayList<>();

    /**
     * 路径前缀。
     */
    private String pathPrefix;

    /**
     * 允许的 HTTP 方法，为空表示不限制。
     */
    private List<HttpMethod> methods = new ArrayList<>();

    /**
     * 是否剥离入口路径前缀。
     */
    private Boolean stripPrefix;

    /**
     * 转发到上游前改写的路径前缀。
     */
    private String rewritePathPrefix;

    /**
     * 转发时追加的请求头。
     */
    private Map<String, String> addHeaders = new LinkedHashMap<>();

    /**
     * 转发时移除的请求头。
     */
    private List<String> removeHeaders = new ArrayList<>();

    /**
     * 目标上游名称。
     */
    private String upstreamName;

    /**
     * 策略名称。
     */
    private List<String> policyNames = new ArrayList<>();
}
