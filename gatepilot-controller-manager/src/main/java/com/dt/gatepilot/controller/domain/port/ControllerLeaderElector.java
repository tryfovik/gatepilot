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
package com.dt.gatepilot.controller.domain.port;

/**
 * controller-manager 主节点选举扩展点。
 */
public interface ControllerLeaderElector {

    /**
     * 当前实例是否为 active leader。
     *
     * @return 是否为 leader
     */
    boolean isLeader();

    /**
     * 当前 leader 标识。
     *
     * @return leader 标识
     */
    String currentLeaderId();
}
