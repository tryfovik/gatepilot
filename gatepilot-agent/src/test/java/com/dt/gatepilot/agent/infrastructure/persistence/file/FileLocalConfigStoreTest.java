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

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件版本地配置存储测试。
 */
class FileLocalConfigStoreTest {

    @TempDir
    private Path tempDir;

    @Test
    void shouldPersistStagedAndLastGoodConfig() {
        FileLocalConfigStore store = new FileLocalConfigStore(objectMapper(), tempDir);

        store.saveStaged(config("v1", "hash-v1"));
        store.promoteLastGood(config("v2", "hash-v2"));

        FileLocalConfigStore reloadedStore = new FileLocalConfigStore(objectMapper(), tempDir);
        assertThat(reloadedStore.loadStaged()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo("v1"));
        assertThat(reloadedStore.loadLastGood()).hasValueSatisfying(config ->
                assertThat(config.getSpec().getVersion()).isEqualTo("v2"));
    }

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 测试映射器保持和自动配置一致
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    private PublishedConfig config(String version, String configHash) {
        PublishedConfig config = new PublishedConfig();
        // 文件存储测试只关心版本和 hash 能否跨实例保留
        config.getSpec().setVersion(version);
        config.getSpec().setConfigHash(configHash);
        return config;
    }
}
