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
package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.proxy.application.dto.ProxyRuntimeSnapshot;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * proxy 运行态原子引用。
 */
public class ProxyRuntimeState {

    private final AtomicReference<CompiledProxyRuntime> current = new AtomicReference<>();

    /**
     * 原子切换运行态。
     *
     * @param runtime 新运行态
     */
    public void switchTo(CompiledProxyRuntime runtime) {
        current.set(runtime);
    }

    /**
     * 查询当前运行态。
     *
     * @return 当前运行态
     */
    public Optional<CompiledProxyRuntime> current() {
        return Optional.ofNullable(current.get());
    }

    /**
     * 查询当前运行态摘要。
     *
     * @return 运行态摘要
     */
    public Optional<ProxyRuntimeSnapshot> snapshot() {
        return current().map(CompiledProxyRuntime::snapshot);
    }
}
