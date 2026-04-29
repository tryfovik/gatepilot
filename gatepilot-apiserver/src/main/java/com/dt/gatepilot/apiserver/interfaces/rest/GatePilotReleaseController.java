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

import com.dt.gatepilot.apiserver.application.dto.CreateReleaseRequest;
import com.dt.gatepilot.apiserver.application.dto.CreateRollbackRequest;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResponse;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResponse;
import com.dt.gatepilot.apiserver.application.service.GatePilotReleaseService;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.getboot.web.api.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * GatePilot 发布请求 API。
 */
@RestController
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.RELEASES)
public class GatePilotReleaseController {

    private final GatePilotReleaseService releaseService;

    /**
     * 创建发布控制器。
     *
     * @param releaseService 发布服务
     */
    public GatePilotReleaseController(GatePilotReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    /**
     * 创建发布请求。
     *
     * @param request 发布请求
     * @return 发布响应
     */
    @PostMapping
    public Mono<ApiResponse<ReleaseResponse>> createRelease(@Valid @RequestBody CreateReleaseRequest request) {
        // apiserver 只创建发布意图，推进交给 controller-manager
        return Mono.just(ApiResponse.success(releaseService.createRelease(request), "发布请求已创建"));
    }

    /**
     * 发布 dry-run 校验。
     *
     * @param request 发布请求
     * @return dry-run 响应
     */
    @PostMapping(GatePilotApiPaths.RELEASE_DRY_RUN)
    public Mono<ApiResponse<ReleaseDryRunResponse>> dryRun(@Valid @RequestBody CreateReleaseRequest request) {
        // dry-run 只做控制面校验，不生成发布事件
        return Mono.just(ApiResponse.success(releaseService.dryRun(request), "dry-run 校验完成"));
    }

    /**
     * 创建回滚请求。
     *
     * @param request 回滚请求
     * @return 回滚发布响应
     */
    @PostMapping(GatePilotApiPaths.RELEASE_ROLLBACK)
    public Mono<ApiResponse<ReleaseResponse>> rollback(@Valid @RequestBody CreateRollbackRequest request) {
        // 回滚也是发布意图，后续异步 reconcile
        return Mono.just(ApiResponse.success(releaseService.createRollback(request), "回滚请求已创建"));
    }
}
