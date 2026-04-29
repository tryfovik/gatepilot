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

import java.util.List;

/**
 * proxy 预编译权重染色策略。
 *
 * @param policyName 策略名称
 * @param weightHashHeaders 权重 hash 请求头
 * @param splits 权重分组
 */
public record CompiledWeightedTrafficPolicy(String policyName,
                                            List<String> weightHashHeaders,
                                            List<CompiledTrafficSplit> splits) {
}
