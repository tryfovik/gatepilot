package com.dt.gatepilot.api.enums;

/**
 * 路由匹配支持的 HTTP 方法。
 */
public enum HttpMethod {

    /**
     * GET 请求。
     */
    GET,

    /**
     * POST 请求。
     */
    POST,

    /**
     * PUT 请求。
     */
    PUT,

    /**
     * PATCH 请求。
     */
    PATCH,

    /**
     * DELETE 请求。
     */
    DELETE,

    /**
     * HEAD 请求。
     */
    HEAD,

    /**
     * OPTIONS 请求。
     */
    OPTIONS,

    /**
     * 任意 HTTP 方法。
     */
    ANY
}
