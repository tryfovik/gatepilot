package com.dt.gatepilot.proxy.domain.runtime;

/**
 * 路由索引键。
 *
 * @param host 请求域名
 * @param pathPrefix 路径前缀
 */
public record RouteMatchKey(String host, String pathPrefix) {
}
