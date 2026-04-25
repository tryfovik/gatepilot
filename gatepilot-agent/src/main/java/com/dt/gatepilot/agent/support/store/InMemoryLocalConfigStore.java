package com.dt.gatepilot.agent.support.store;

import com.dt.gatepilot.agent.spi.LocalConfigStore;
import com.dt.gatepilot.api.resource.publish.PublishedConfig;
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
