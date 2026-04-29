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
package com.dt.gatepilot.agent.domain.port;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;

/**
 * agent 本地配置存储扩展点。
 */
public interface LocalConfigStore {

    /**
     * 保存 staged 配置。
     *
     * @param config 已发布配置
     */
    void saveStaged(PublishedConfig config);

    /**
     * 查询 staged 配置。
     *
     * @return staged 配置
     */
    Optional<PublishedConfig> loadStaged();

    /**
     * 将指定配置晋升为 last-good。
     *
     * @param config 已发布配置
     */
    void promoteLastGood(PublishedConfig config);

    /**
     * 查询 last-good 配置。
     *
     * @return last-good 配置
     */
    Optional<PublishedConfig> loadLastGood();
}
