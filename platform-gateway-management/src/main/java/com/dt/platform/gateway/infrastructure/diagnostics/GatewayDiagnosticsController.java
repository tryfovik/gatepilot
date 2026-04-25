package com.dt.platform.gateway.infrastructure.diagnostics;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 网关内部诊断 API。
 */
@RestController
public class GatewayDiagnosticsController {

    private final GatewayDiagnosticsService diagnosticsService;

    /**
     * 创建诊断控制器。
     *
     * @param diagnosticsService 诊断服务
     */
    public GatewayDiagnosticsController(GatewayDiagnosticsService diagnosticsService) {
        this.diagnosticsService = diagnosticsService;
    }

    /**
     * 诊断模拟请求的路由命中和治理策略。
     *
     * @param request 诊断请求
     * @return 诊断结果
     */
    @PostMapping("${platform.gateway.internal-prefix:/internal}/_platform-gateway/diagnostics/route")
    public Mono<GatewayDiagnosticsService.GatewayDiagnosticsView> diagnoseRoute(
            @RequestBody GatewayDiagnosticsService.GatewayDiagnosticsRequest request) {
        return Mono.just(diagnosticsService.diagnose(request));
    }
}
