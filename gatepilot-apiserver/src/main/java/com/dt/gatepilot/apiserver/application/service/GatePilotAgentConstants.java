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
 * agent 协议服务常量
 */
public final class GatePilotAgentConstants {

    /**
     * PublishedConfig 拉取分页大小
     */
    public static final int PUBLISHED_CONFIG_PAGE_LIMIT = 500;

    /**
     * 没有可拉取配置提示
     */
    public static final String MESSAGE_NO_PUBLISHED_CONFIG = "暂无可拉取的 PublishedConfig";

    /**
     * 发现新配置提示
     */
    public static final String MESSAGE_CONFIG_CHANGED = "发现新配置";

    /**
     * 当前配置已是最新提示
     */
    public static final String MESSAGE_CONFIG_NOT_CHANGED = "当前配置已是最新";

    private GatePilotAgentConstants() {
        // agent 协议服务常量不允许实例化
    }
}
