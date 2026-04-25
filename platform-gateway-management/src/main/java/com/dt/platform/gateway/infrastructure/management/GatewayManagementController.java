package com.dt.platform.gateway.infrastructure.management;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关管理面内部 API。
 */
@RestController
public class GatewayManagementController {

    private final GatewayManagementService managementService;

    /**
     * 创建管理面控制器。
     *
     * @param managementService 配置治理服务
     */
    public GatewayManagementController(GatewayManagementService managementService) {
        this.managementService = managementService;
    }

    /**
     * 导出当前生效配置。
     *
     * @return 当前配置
     */
    @GetMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/effective")
    public Mono<GatewayManagementService.GatewayConfigExportView> exportConfig() {
        return Mono.just(managementService.exportConfig());
    }

    /**
     * dry-run 校验候选配置。
     *
     * @param candidate 候选配置
     * @return 校验结果
     */
    @PostMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/validate")
    public Mono<GatewayManagementService.GatewayConfigValidationView> validateConfig(
            @RequestBody GatewayProperties candidate) {
        return Mono.just(managementService.validate(candidate));
    }

    /**
     * 对比当前配置和候选配置。
     *
     * @param candidate 候选配置
     * @return 配置差异
     */
    @PostMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/diff")
    public Mono<GatewayManagementService.GatewayConfigDiffView> diffConfig(
            @RequestBody GatewayProperties candidate) {
        return Mono.just(managementService.diff(candidate));
    }

    /**
     * 保存候选配置版本快照。
     *
     * @param request 快照请求
     * @return 配置版本快照
     */
    @PostMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/versions")
    public Mono<GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord> createConfigVersion(
            @RequestBody GatewayManagementService.GatewayConfigSnapshotRequest request) {
        return Mono.just(managementService.createCandidateSnapshot(request));
    }

    /**
     * 查询配置版本快照列表。
     *
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 配置版本快照
     */
    @GetMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/versions")
    public Mono<List<GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord>> configVersions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return Mono.just(managementService.listConfigVersions(status, limit));
    }

    /**
     * 查询单个配置版本快照。
     *
     * @param versionId 版本号
     * @return 配置版本快照
     */
    @GetMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/versions/{versionId}")
    public Mono<GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord> configVersion(
            @PathVariable String versionId) {
        try {
            return Mono.just(managementService.getConfigVersion(versionId));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    /**
     * 查询配置发布事件记录。
     *
     * @param action 动作过滤
     * @param status 状态过滤
     * @param limit 返回条数
     * @return 配置发布事件记录
     */
    @GetMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/config/releases")
    public Mono<List<GatewayConfigSnapshotRepository.GatewayConfigReleaseRecord>> configReleaseRecords(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return Mono.just(managementService.listReleaseRecords(action, status, limit));
    }
}
