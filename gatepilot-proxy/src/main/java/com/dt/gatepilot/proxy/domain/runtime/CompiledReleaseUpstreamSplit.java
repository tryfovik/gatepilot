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

/**
 * proxy 预编译发布上游分流。
 *
 * @param target 目标分组
 * @param color 流量颜色
 * @param upstreamName 上游名称
 */
public record CompiledReleaseUpstreamSplit(String target, String color, String upstreamName) {

    /**
     * 判断流量颜色是否命中分流。
     *
     * @param trafficColor 流量颜色
     * @return 是否命中
     */
    public boolean matches(String trafficColor) {
        return trafficColor.equals(color) || trafficColor.equals(target);
    }
}
