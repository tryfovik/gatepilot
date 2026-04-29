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
package com.dt.gatepilot.proxy.domain.port;

import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import java.util.List;

/**
 * 上游实例发现注册表。
 */
public interface UpstreamDiscoveryRegistry {

    /**
     * 根据新运行态刷新订阅。
     *
     * @param runtime 新运行态
     */
    void refresh(CompiledProxyRuntime runtime);

    /**
     * 查询上游当前实例。
     *
     * @param upstream 已编译上游
     * @return 当前可用实例
     */
    List<CompiledUpstream.CompiledEndpoint> instances(CompiledUpstream upstream);
}
