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
 * 网关节点状态。
 */
public enum NodePhase {

    /**
     * 已注册但还未准备好。
     */
    REGISTERED,

    /**
     * 节点健康且可接流量。
     */
    READY,

    /**
     * 节点未就绪。
     */
    NOT_READY,

    /**
     * 节点正在摘流。
     */
    DRAINING,

    /**
     * 节点离线。
     */
    OFFLINE
}
