package com.dt.platform.gateway.infrastructure.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 网关访问审计查询 API。
 */
@RestController
public class GatewayAccessAuditController {

    private final GatewayAccessAuditRepository auditRepository;

    /**
     * 创建访问审计查询控制器。
     *
     * @param auditRepository 审计仓库
     */
    public GatewayAccessAuditController(GatewayAccessAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * 查询最近访问审计事件。
     *
     * @param traceId TraceId
     * @param projectKey 项目标识
     * @param routeKey 路由标识
     * @param clientIp 客户端地址
     * @param status 响应状态码
     * @param trafficColor 流量颜色
     * @param releaseVariant 发布变体
     * @param limit 返回条数
     * @return 审计事件列表
     */
    @GetMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/audits")
    public Mono<List<GatewayAccessAuditEvent>> audits(@RequestParam(required = false) String traceId,
                                                      @RequestParam(required = false) String projectKey,
                                                      @RequestParam(required = false) String routeKey,
                                                      @RequestParam(required = false) String clientIp,
                                                      @RequestParam(required = false) Integer status,
                                                      @RequestParam(required = false) String trafficColor,
                                                      @RequestParam(required = false) String releaseVariant,
                                                      @RequestParam(required = false) Integer limit) {
        return Mono.just(auditRepository.search(new GatewayAccessAuditRepository.AuditQuery(
                traceId,
                projectKey,
                routeKey,
                clientIp,
                status,
                trafficColor,
                releaseVariant,
                limit
        )));
    }
}
