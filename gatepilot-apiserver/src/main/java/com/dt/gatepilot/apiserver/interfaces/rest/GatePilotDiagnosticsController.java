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

import com.dt.gatepilot.apiserver.application.dto.RouteCatalogResponse;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsRequest;
import com.dt.gatepilot.apiserver.application.dto.RouteDiagnosticsResponse;
import com.dt.gatepilot.apiserver.application.service.GatePilotDiagnosticsService;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.getboot.web.api.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * GatePilot 诊断查询 API。
 */
@RestController
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.DIAGNOSTICS)
public class GatePilotDiagnosticsController {

    private final GatePilotDiagnosticsService diagnosticsService;

    /**
     * 创建诊断查询控制器。
     *
     * @param diagnosticsService 诊断查询服务
     */
    public GatePilotDiagnosticsController(GatePilotDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * 查询路由目录。
     *
     * @param namespace 命名空间
     * @param projectName 项目名称
     * @param version 发布版本
     * @param configShard 配置分片
     * @return 路由目录
     */
    @GetMapping(GatePilotApiPaths.DIAGNOSTICS_ROUTE_CATALOG)
    public Mono<ApiResponse<RouteCatalogResponse>> routeCatalog(
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String version,
            @RequestParam(required = false) String configShard) {
        // 诊断查询只读取 apiserver 已发布配置，不访问 proxy
        return Mono.just(ApiResponse.success(diagnosticsService.catalog(namespace, projectName, version, configShard)));
    }

    /**
     * 诊断一次模拟请求。
     *
     * @param request 诊断请求
     * @return 诊断结果
     */
    @PostMapping(GatePilotApiPaths.DIAGNOSTICS_ROUTE)
    public Mono<ApiResponse<RouteDiagnosticsResponse>> diagnoseRoute(@RequestBody RouteDiagnosticsRequest request) {
        // 模拟诊断不产生发布副作用
        return Mono.just(ApiResponse.success(diagnosticsService.diagnose(request)));
    }
}
