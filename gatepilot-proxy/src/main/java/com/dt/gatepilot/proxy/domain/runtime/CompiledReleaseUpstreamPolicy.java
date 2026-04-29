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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译发布上游策略。
 */
@Data
public class CompiledReleaseUpstreamPolicy {

    /**
     * 发布上游分流列表。
     */
    private List<CompiledReleaseUpstreamSplit> splits = new ArrayList<>();
}
