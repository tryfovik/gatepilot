package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.application.dto.ConfigDiffResult;
import com.dt.gatepilot.apiserver.application.service.GatePilotConfigSnapshotService;
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
