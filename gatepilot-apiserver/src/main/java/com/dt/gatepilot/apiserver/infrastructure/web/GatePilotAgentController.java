package com.dt.gatepilot.apiserver.infrastructure.web;

import com.dt.gatepilot.api.resource.node.GatewayNode;
import com.dt.gatepilot.apiserver.api.request.AgentApplyResultReportRequest;
import com.dt.gatepilot.apiserver.api.request.AgentConfigPullRequest;
import com.dt.gatepilot.apiserver.api.request.AgentHeartbeatRequest;
import com.dt.gatepilot.apiserver.api.request.AgentRegisterRequest;
import com.dt.gatepilot.apiserver.api.response.AgentConfigPullResponse;
import com.dt.gatepilot.apiserver.support.service.GatePilotAgentService;
import com.getboot.web.api.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * agent 与 apiserver 的同步协议 API。
 */
@RestController
@RequestMapping("/api/gatepilot/v1/agents")
public class GatePilotAgentController {

    private final GatePilotAgentService agentService;

    /**
     * 创建 agent 控制器。
     *
     * @param agentService agent 服务
     */
    public GatePilotAgentController(GatePilotAgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 注册节点。
     *
     * @param request 注册请求
     * @return 节点资源
     */
    @PostMapping("/register")
    public Mono<ApiResponse<GatewayNode>> register(@Valid @RequestBody AgentRegisterRequest request) {
        return Mono.just(ApiResponse.success(agentService.register(request), "节点注册成功"));
    }

    /**
     * 节点心跳。
     *
     * @param request 心跳请求
     * @return 节点资源
     */
    @PostMapping("/heartbeat")
    public Mono<ApiResponse<GatewayNode>> heartbeat(@Valid @RequestBody AgentHeartbeatRequest request) {
        return Mono.just(ApiResponse.success(agentService.heartbeat(request), "心跳已接收"));
    }

    /**
     * 拉取已发布配置。
     *
     * @param request 拉取请求
     * @return 已发布配置
     */
    @PostMapping("/configs/pull")
    public Mono<ApiResponse<AgentConfigPullResponse>> pullConfig(@Valid @RequestBody AgentConfigPullRequest request) {
        return Mono.just(ApiResponse.success(agentService.pullConfig(request)));
    }

    /**
     * 上报配置应用结果。
     *
     * @param request 上报请求
     * @return 节点资源
     */
    @PostMapping("/apply-results")
    public Mono<ApiResponse<GatewayNode>> reportApplyResult(
            @Valid @RequestBody AgentApplyResultReportRequest request) {
        return Mono.just(ApiResponse.success(agentService.reportApplyResult(request), "应用结果已接收"));
    }
}
