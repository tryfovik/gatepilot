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
package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.application.dto.ConfigDiffResult;
import com.dt.gatepilot.apiserver.application.dto.ConfigSnapshotSummaryResponse;
import com.dt.gatepilot.apiserver.application.service.GatePilotConfigSnapshotService;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.getboot.web.api.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 配置版本快照 API。
 */
@RestController
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.CONFIG_SNAPSHOTS)
public class GatePilotConfigSnapshotController {

    private final GatePilotConfigSnapshotService snapshotService;

    /**
     * 创建配置版本快照控制器。
     *
     * @param snapshotService 配置快照服务
     */
    public GatePilotConfigSnapshotController(GatePilotConfigSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * 查询配置版本快照摘要列表。
     *
     * @param namespace 命名空间
     * @param projectName 项目名称
     * @param configShard 配置分片
     * @param cursor 游标
     * @param limit 返回条数
     * @return 快照摘要列表
     */
    @GetMapping
    public Mono<ApiResponse<CursorPage<ConfigSnapshotSummaryResponse>>> list(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String configShard,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        // 摘要列表不给控制台返回完整 PublishedConfig，避免页面加载过重
        return Mono.just(ApiResponse.success(snapshotService.listSummaries(
                namespace, projectName, configShard, cursor, limit)));
    }

    /**
     * 比较两个配置版本。
     *
     * @param namespace 命名空间
     * @param baseVersion 基线版本
     * @param targetVersion 目标版本
     * @param configShard 配置分片
     * @return diff 响应
     */
    @GetMapping(GatePilotApiPaths.CONFIG_SNAPSHOT_DIFF)
    public Mono<ApiResponse<ConfigDiffResult>> diff(@RequestParam String namespace,
                                                      @RequestParam String baseVersion,
                                                      @RequestParam String targetVersion,
                                                      @RequestParam(required = false) String configShard) {
        // diff 基于持久化快照，不读取 proxy 本地运行态
        return Mono.just(ApiResponse.success(snapshotService.diff(namespace, baseVersion, targetVersion, configShard)));
    }
}
