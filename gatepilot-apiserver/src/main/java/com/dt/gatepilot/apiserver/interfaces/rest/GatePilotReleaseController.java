package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.command.CreateRollbackCommand;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.application.dto.ReleaseResult;
import com.dt.gatepilot.apiserver.application.service.GatePilotReleaseService;
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
    public Mono<ApiResponse<ReleaseResult>> createRelease(@Valid @RequestBody CreateReleaseCommand request) {
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
    public Mono<ApiResponse<ReleaseDryRunResult>> dryRun(@Valid @RequestBody CreateReleaseCommand request) {
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
    public Mono<ApiResponse<ReleaseResult>> rollback(@Valid @RequestBody CreateRollbackCommand request) {
        // 回滚也是发布意图，后续异步 reconcile
        return Mono.just(ApiResponse.success(releaseService.createRollback(request), "回滚请求已创建"));
    }
}
