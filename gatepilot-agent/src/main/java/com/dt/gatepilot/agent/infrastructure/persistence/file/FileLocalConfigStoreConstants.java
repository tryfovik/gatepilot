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
package com.dt.gatepilot.agent.infrastructure.persistence.file;

/**
 * 文件本地配置存储常量。
 */
public final class FileLocalConfigStoreConstants {

    /**
     * staged 配置文件名。
     */
    public static final String STAGED_CONFIG_FILE_NAME = "staged-config.json";

    /**
     * last-good 配置文件名。
     */
    public static final String LAST_GOOD_CONFIG_FILE_NAME = "last-good-config.json";

    /**
     * 临时文件后缀。
     */
    public static final String TEMP_FILE_SUFFIX = ".tmp";

    /**
     * 本地配置读取失败提示。
     */
    public static final String MESSAGE_READ_CONFIG_FAILED = "读取 agent 本地配置失败";

    /**
     * 本地配置写入失败提示。
     */
    public static final String MESSAGE_WRITE_CONFIG_FAILED = "写入 agent 本地配置失败";

    private FileLocalConfigStoreConstants() {
        // 文件本地配置存储常量不允许实例化
    }
}
