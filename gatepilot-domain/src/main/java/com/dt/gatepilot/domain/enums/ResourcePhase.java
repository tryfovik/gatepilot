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
package com.dt.gatepilot.domain.enums;

/**
 * 声明式资源的通用生命周期状态。
 */
public enum ResourcePhase {

    /**
     * 等待控制面处理。
     */
    PENDING,

    /**
     * 当前资源已经生效。
     */
    ACTIVE,

    /**
     * 当前资源部分可用或存在告警。
     */
    DEGRADED,

    /**
     * 当前资源处理失败。
     */
    FAILED,

    /**
     * 当前资源已经标记删除。
     */
    DELETED
}
