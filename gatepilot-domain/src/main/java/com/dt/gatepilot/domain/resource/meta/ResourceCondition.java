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
package com.dt.gatepilot.domain.resource.meta;

import java.time.Instant;
import lombok.Data;

/**
 * 资源状态条件，用于解释资源为什么健康、异常或等待中。
 */
@Data
public class ResourceCondition {

    /**
     * 条件类型，例如 Ready、Synced、Published。
     */
    private String type;

    /**
     * 条件状态，建议使用 True、False、Unknown。
     */
    private String status;

    /**
     * 机器可读的原因码。
     */
    private String reason;

    /**
     * 面向排障人员的说明。
     */
    private String message;

    /**
     * 条件最近一次变化时间。
     */
    private Instant lastTransitionTime;

    /**
     * 当前条件观测到的资源 generation。
     */
    private Long observedGeneration;
}
