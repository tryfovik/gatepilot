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

import com.dt.gatepilot.agent.domain.port.LocalConfigStore;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * 文件版本地配置存储。
 */
public class FileLocalConfigStore implements LocalConfigStore {

    private final ObjectMapper objectMapper;

    private final Path directory;

    private final Path stagedFile;

    private final Path lastGoodFile;

    /**
     * 创建文件版本地配置存储。
     *
     * @param objectMapper JSON 映射器
     * @param directory 本地配置目录
     */
    public FileLocalConfigStore(ObjectMapper objectMapper, Path directory) {
        this.objectMapper = objectMapper;
        this.directory = directory;
        this.stagedFile = directory.resolve(FileLocalConfigStoreConstants.STAGED_CONFIG_FILE_NAME);
        this.lastGoodFile = directory.resolve(FileLocalConfigStoreConstants.LAST_GOOD_CONFIG_FILE_NAME);
    }

    @Override
    public synchronized void saveStaged(PublishedConfig config) {
        writeConfig(stagedFile, config);
    }

    @Override
    public synchronized Optional<PublishedConfig> loadStaged() {
        return readConfig(stagedFile);
    }

    @Override
    public synchronized void promoteLastGood(PublishedConfig config) {
        writeConfig(lastGoodFile, config);
    }

    @Override
    public synchronized Optional<PublishedConfig> loadLastGood() {
        return readConfig(lastGoodFile);
    }

    private Optional<PublishedConfig> readConfig(Path file) {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        try {
            // 文件存储只反序列化 PublishedConfig，不承载发布决策
            return Optional.of(objectMapper.readValue(file.toFile(), PublishedConfig.class));
        } catch (IOException exception) {
            throw new IllegalStateException(FileLocalConfigStoreConstants.MESSAGE_READ_CONFIG_FAILED
                    + ": " + file, exception);
        }
    }

    private void writeConfig(Path file, PublishedConfig config) {
        try {
            Files.createDirectories(directory);
            Path tempFile = Files.createTempFile(directory, file.getFileName().toString(),
                    FileLocalConfigStoreConstants.TEMP_FILE_SUFFIX);
            objectMapper.writeValue(tempFile.toFile(), config);
            moveAtomically(tempFile, file);
        } catch (IOException exception) {
            throw new IllegalStateException(FileLocalConfigStoreConstants.MESSAGE_WRITE_CONFIG_FAILED
                    + ": " + file, exception);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            // 支持原子移动时避免 agent 重启读到半文件
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
