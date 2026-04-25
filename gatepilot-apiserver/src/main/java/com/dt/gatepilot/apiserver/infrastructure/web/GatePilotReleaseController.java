package com.dt.gatepilot.apiserver.infrastructure.web;

import com.dt.gatepilot.apiserver.api.request.ReleaseRequest;
import com.dt.gatepilot.apiserver.api.request.RollbackRequest;
import com.dt.gatepilot.apiserver.api.response.ReleaseDryRunResponse;
import com.dt.gatepilot.apiserver.api.response.ReleaseResponse;
import com.dt.gatepilot.apiserver.support.service.GatePilotReleaseService;
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
@RequestMapping("/api/gatepilot/v1/releases")
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
    public Mono<ApiResponse<ReleaseResponse>> createRelease(@Valid @RequestBody ReleaseRequest request) {
        return Mono.just(ApiResponse.success(releaseService.createRelease(request), "发布请求已创建"));
    }

    /**
     * 发布 dry-run 校验。
     *
     * @param request 发布请求
     * @return dry-run 响应
     */
    @PostMapping("/dry-run")
    public Mono<ApiResponse<ReleaseDryRunResponse>> dryRun(@Valid @RequestBody ReleaseRequest request) {
        return Mono.just(ApiResponse.success(releaseService.dryRun(request), "dry-run 校验完成"));
    }

    /**
     * 创建回滚请求。
     *
     * @param request 回滚请求
     * @return 回滚发布响应
     */
    @PostMapping("/rollback")
    public Mono<ApiResponse<ReleaseResponse>> rollback(@Valid @RequestBody RollbackRequest request) {
        return Mono.just(ApiResponse.success(releaseService.createRollback(request), "回滚请求已创建"));
    }
}
