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

import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateApplyResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDefaultsResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDryRunResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplatePreviewResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateRenderRequest;
import com.dt.gatepilot.apiserver.application.service.ProjectTemplateConstants;
import com.dt.gatepilot.apiserver.application.service.ProjectTemplateService;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.getboot.web.api.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * GatePilot 模板 API
 */
@RestController
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.TEMPLATES)
public class GatePilotTemplateController {

    private final ProjectTemplateService templateService;

    /**
     * 创建模板控制器
     *
     * @param templateService 模板服务
     */
    public GatePilotTemplateController(ProjectTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 查询项目模板默认配置
     *
     * @return 默认配置响应
     */
    @GetMapping(GatePilotApiPaths.TEMPLATE_PROJECT_DEFAULTS)
    public Mono<ApiResponse<ProjectTemplateDefaultsResponse>> projectDefaults() {
        // 默认值和选项集中由 apiserver 提供，避免前端散落配置规则
        return Mono.just(ApiResponse.success(templateService.defaults(),
                ProjectTemplateConstants.MESSAGE_TEMPLATE_DEFAULTS_DONE));
    }

    /**
     * 预览项目模板
     *
     * @param request 模板 values
     * @return 预览响应
     */
    @PostMapping(GatePilotApiPaths.TEMPLATE_PROJECT_PREVIEW)
    public Mono<ApiResponse<ProjectTemplatePreviewResponse>> previewProject(
            @Valid @RequestBody ProjectTemplateRenderRequest request) {
        // 预览只渲染资源和 diff，不写入存储
        return Mono.just(ApiResponse.success(templateService.preview(request),
                ProjectTemplateConstants.MESSAGE_TEMPLATE_PREVIEW_DONE));
    }

    /**
     * dry-run 项目模板
     *
     * @param request 模板 values
     * @return dry-run 响应
     */
    @PostMapping(GatePilotApiPaths.TEMPLATE_PROJECT_DRY_RUN)
    public Mono<ApiResponse<ProjectTemplateDryRunResponse>> dryRunProject(
            @Valid @RequestBody ProjectTemplateRenderRequest request) {
        // dry-run 校验模板和渲染结果，不创建发布事件
        return Mono.just(ApiResponse.success(templateService.dryRun(request),
                ProjectTemplateConstants.MESSAGE_TEMPLATE_DRY_RUN_DONE));
    }

    /**
     * 保存项目模板资源
     *
     * @param request 模板 values
     * @return 保存响应
     */
    @PostMapping(GatePilotApiPaths.TEMPLATE_PROJECT_APPLY)
    public Mono<ApiResponse<ProjectTemplateApplyResponse>> applyProject(
            @Valid @RequestBody ProjectTemplateRenderRequest request) {
        // 保存模板资源仍走 apiserver 资源存储，不触碰 agent / proxy
        return Mono.just(ApiResponse.success(templateService.apply(request),
                ProjectTemplateConstants.MESSAGE_TEMPLATE_APPLY_DONE));
    }
}
