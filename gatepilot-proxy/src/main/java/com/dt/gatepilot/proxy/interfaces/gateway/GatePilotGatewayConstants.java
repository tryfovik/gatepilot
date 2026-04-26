package com.dt.gatepilot.proxy.interfaces.gateway;

import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;

/**
 * GatePilot Spring Cloud Gateway 适配常量。
 */
public final class GatePilotGatewayConstants {

    /**
     * GatePilot 数据面兜底路由标识。
     */
    public static final String PROXY_ROUTE_ID = "gatepilot-proxy";

    /**
     * GatePilot 数据面兜底路由顺序。
     */
    public static final int PROXY_ROUTE_ORDER = -100;

    /**
     * 兜底路由占位上游。
     */
    public static final String PROXY_PLACEHOLDER_URI = "http://gatepilot.local";

    /**
     * GatePilot 治理过滤器顺序。
     */
    public static final int GATEWAY_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    /**
     * SCG 重试退避倍率。
     */
    public static final int RETRY_BACKOFF_FACTOR = 2;

    /**
     * SCG 重试退避是否基于上一次结果。
     */
    public static final boolean RETRY_BACKOFF_BASED_ON_PREVIOUS = false;

    /**
     * JSON 序列化失败兜底响应。
     */
    public static final String JSON_SERIALIZE_ERROR_BODY = "{\"code\":500,\"message\":\"网关响应序列化失败\"}";

    private GatePilotGatewayConstants() {
        // SCG 适配常量不允许实例化
    }
}
