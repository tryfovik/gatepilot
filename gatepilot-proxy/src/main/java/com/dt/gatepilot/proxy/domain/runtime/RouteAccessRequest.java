package com.dt.gatepilot.proxy.domain.runtime;

/**
 * 路由访问判断请求。
 *
 * @param host 请求域名
 * @param path 请求路径
 * @param method HTTP 方法
 */
public record RouteAccessRequest(String host, String path, String method) {
}
