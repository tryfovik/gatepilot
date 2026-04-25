package com.dt.gatepilot.proxy.domain.runtime;

import java.util.function.Function;

/**
 * 流量染色解析所需的请求上下文。
 *
 * @param host 请求域名
 * @param path 请求路径
 * @param rawQuery 原始 Query
 * @param headerReader 请求头读取器
 * @param cookieReader Cookie 读取器
 * @param queryReader Query 读取器
 * @param remoteAddress 远端地址
 */
public record TrafficColorRequest(String host,
                                  String path,
                                  String rawQuery,
                                  Function<String, String> headerReader,
                                  Function<String, String> cookieReader,
                                  Function<String, String> queryReader,
                                  String remoteAddress) {

    /**
     * 读取请求头。
     *
     * @param name 请求头名称
     * @return 请求头值
     */
    public String header(String name) {
        return headerReader == null ? null : headerReader.apply(name);
    }

    /**
     * 读取 Cookie。
     *
     * @param name Cookie 名称
     * @return Cookie 值
     */
    public String cookie(String name) {
        return cookieReader == null ? null : cookieReader.apply(name);
    }

    /**
     * 读取 Query 参数。
     *
     * @param name Query 参数名称
     * @return Query 参数值
     */
    public String query(String name) {
        return queryReader == null ? null : queryReader.apply(name);
    }
}
