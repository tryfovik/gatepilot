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
package com.dt.gatepilot.agent.infrastructure.persistence.memory;

import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 内存版本地配置存储，用于开发期验证 staged / last-good 流程。
 */
public class InMemoryLocalConfigStore implements LocalConfigStore {

    private final AtomicReference<PublishedConfig> staged = new AtomicReference<>();

    private final AtomicReference<PublishedConfig> lastGood = new AtomicReference<>();

    @Override
    public void saveStaged(PublishedConfig config) {
        staged.set(config);
    }

    @Override
    public Optional<PublishedConfig> loadStaged() {
        return Optional.ofNullable(staged.get());
    }

    @Override
    public void promoteLastGood(PublishedConfig config) {
        lastGood.set(config);
    }

    @Override
    public Optional<PublishedConfig> loadLastGood() {
        return Optional.ofNullable(lastGood.get());
    }
}
