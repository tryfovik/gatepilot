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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译流量染色策略。
 */
@Data
public class CompiledTrafficColorPolicy {

    /**
     * 是否信任请求头染色。
     */
    private boolean trustRequestHeader;

    /**
     * 染色请求头名称。
     */
    private String headerName = TrafficColorConstants.DEFAULT_HEADER_NAME;

    /**
     * 默认流量颜色。
     */
    private String defaultColor = TrafficColorConstants.DEFAULT_COLOR;

    /**
     * 显式染色规则。
     */
    private List<CompiledTrafficColorRule> rules = new ArrayList<>();

    /**
     * 权重染色策略。
     */
    private List<CompiledWeightedTrafficPolicy> weightedPolicies = new ArrayList<>();
}
