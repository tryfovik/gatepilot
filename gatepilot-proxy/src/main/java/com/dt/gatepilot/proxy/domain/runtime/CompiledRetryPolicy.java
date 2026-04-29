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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译重试策略。
 */
@Data
public class CompiledRetryPolicy {

    /**
     * 策略名称。
     */
    private String name;

    /**
     * 是否启用。
     */
    private boolean enabled;

    /**
     * 最大尝试次数。
     */
    private int maxAttempts;

    /**
     * 可重试状态码。
     */
    private List<Integer> statuses = new ArrayList<>();

    /**
     * 首次退避时间。
     */
    private Duration firstBackoff;

    /**
     * 最大退避时间。
     */
    private Duration maxBackoff;
}
