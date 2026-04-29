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
package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 发布 dry-run 校验响应。
 */
@Data
public class ReleaseDryRunResponse {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 预估发布版本。
     */
    private String version;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 校验是否通过。
     */
    private boolean passed;

    /**
     * 路由数量。
     */
    private int routeCount;

    /**
     * 上游数量。
     */
    private int upstreamCount;

    /**
     * 策略数量。
     */
    private int policyCount;

    /**
     * dry-run 时间。
     */
    private Instant checkedAt;

    /**
     * 校验消息。
     */
    private List<DryRunMessage> messages = new ArrayList<>();

    /**
     * dry-run 校验消息。
     */
    @Data
    public static class DryRunMessage {

        /**
         * 级别：INFO / WARN / ERROR。
         */
        private String level;

        /**
         * 机器可读原因码。
         */
        private String reason;

        /**
         * 说明。
         */
        private String message;
    }
}
