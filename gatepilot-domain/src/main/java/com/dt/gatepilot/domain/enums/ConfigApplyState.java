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
 * 已发布配置在节点上的应用状态。
 */
public enum ConfigApplyState {

    /**
     * 等待应用。
     */
    PENDING,

    /**
     * 已写入暂存区。
     */
    STAGED,

    /**
     * 已应用到 proxy。
     */
    APPLIED,

    /**
     * 应用失败。
     */
    FAILED,

    /**
     * 已回滚到 last-good。
     */
    ROLLED_BACK
}
