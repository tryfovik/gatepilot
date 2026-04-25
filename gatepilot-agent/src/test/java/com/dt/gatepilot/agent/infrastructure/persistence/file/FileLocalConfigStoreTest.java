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
