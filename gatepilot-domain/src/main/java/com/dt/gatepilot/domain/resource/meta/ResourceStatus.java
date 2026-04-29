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

import com.dt.gatepilot.domain.enums.ResourcePhase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * GatePilot 资源通用实际状态，对应声明式资源中的 status。
 */
@Data
public class ResourceStatus {

    /**
     * 控制面已经处理到的 generation。
     */
    private Long observedGeneration;

    /**
     * 当前资源生命周期状态。
     */
    private ResourcePhase phase;

    /**
     * 状态条件列表，用于页面诊断和自动化判断。
     */
    private List<ResourceCondition> conditions = new ArrayList<>();

    /**
     * 最近一次状态变化时间。
     */
    private Instant lastTransitionTime;

    /**
     * 最近一次错误码。
     */
    private String reason;

    /**
     * 最近一次错误或告警说明。
     */
    private String message;
}
