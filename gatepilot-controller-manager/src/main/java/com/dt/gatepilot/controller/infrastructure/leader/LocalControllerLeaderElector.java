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
package com.dt.gatepilot.controller.infrastructure.leader;

import com.dt.gatepilot.controller.domain.port.ControllerLeaderElector;

/**
 * 本地单实例 leader 选举器。
 */
public class LocalControllerLeaderElector implements ControllerLeaderElector {

    private final String controllerId;

    /**
     * 创建本地 leader 选举器。
     *
     * @param controllerId controller-manager 实例标识
     */
    public LocalControllerLeaderElector(String controllerId) {
        this.controllerId = controllerId;
    }

    @Override
    public boolean isLeader() {
        return true;
    }

    @Override
    public String currentLeaderId() {
        return controllerId;
    }
}
