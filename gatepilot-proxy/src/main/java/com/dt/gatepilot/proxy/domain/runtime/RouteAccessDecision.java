package com.dt.gatepilot.proxy.domain.runtime;

import java.util.List;

/**
 * 路由访问判断结果。
 *
 * @param matched 是否命中路由
 * @param route 命中的路由
 * @param methodAllowed HTTP 方法是否允许
 * @param allowedMethods 允许的方法列表
 * @param authenticationRequired 是否需要认证
 */
public record RouteAccessDecision(boolean matched,
                                  CompiledRoute route,
                                  boolean methodAllowed,
                                  List<String> allowedMethods,
                                  boolean authenticationRequired) {

    /**
     * 未命中路由。
     *
     * @return 判断结果
     */
    public static RouteAccessDecision notMatched() {
        return new RouteAccessDecision(false, null, true, List.of(), false);
    }
}
