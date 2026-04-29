/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
