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
package com.dt.gatepilot.apiserver.application.service;

/**
 * 配置快照常量。
 */
public final class ConfigSnapshotConstants {

    /**
     * 摘要列表默认页大小。
     */
    public static final int DEFAULT_SUMMARY_LIMIT = 50;

    /**
     * 摘要列表最大页大小。
     */
    public static final int MAX_SUMMARY_LIMIT = 500;

    /**
     * 快照扫描页大小。
     */
    public static final int SNAPSHOT_SCAN_LIMIT = 500;

    /**
     * 快照复制失败提示。
     */
    public static final String MESSAGE_SNAPSHOT_COPY_FAILED = "配置快照复制失败";

    private ConfigSnapshotConstants() {
        // 配置快照常量不允许实例化
    }
}
