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
package com.dt.gatepilot.apiserver.domain.audit;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 运行审计查询条件。
 */
@Data
public class RuntimeAuditQuery {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 路由标识。
     */
    private String routeId;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * TraceId。
     */
    private String traceId;

    /**
     * 执行结果。
     */
    private String outcome;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime endedAt;

    /**
     * 游标。
     */
    private String cursor;

    /**
     * 返回条数。
     */
    private int limit;
}
