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
package com.dt.gatepilot.controller.application.service;

/**
 * 发布事件工厂常量。
 */
public final class ReleaseEventFactoryConstants {

    /**
     * 发布事件名称前缀。
     */
    public static final String EVENT_NAME_PREFIX = "event-";

    /**
     * PublishedConfig 生成消息。
     */
    public static final String MESSAGE_PUBLISHED_CONFIG_GENERATED = "controller-manager 已生成 PublishedConfig";

    /**
     * 回滚 PublishedConfig 生成消息。
     */
    public static final String MESSAGE_ROLLBACK_CONFIG_GENERATED = "controller-manager 已生成回滚 PublishedConfig";

    private ReleaseEventFactoryConstants() {
        // 发布事件工厂常量不允许实例化
    }
}
