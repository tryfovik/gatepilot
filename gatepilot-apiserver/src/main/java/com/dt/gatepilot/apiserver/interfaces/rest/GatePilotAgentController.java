package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.domain.resource.node.GatewayNode;
import com.dt.gatepilot.apiserver.application.command.ReportAgentApplyResultCommand;
import com.dt.gatepilot.apiserver.application.command.PullAgentConfigCommand;
import com.dt.gatepilot.apiserver.application.command.AgentHeartbeatCommand;
import com.dt.gatepilot.apiserver.application.command.RegisterAgentCommand;
import com.dt.gatepilot.apiserver.application.dto.AgentConfigPullResult;
import com.dt.gatepilot.apiserver.application.service.GatePilotAgentService;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
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
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.AGENTS)
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
    @PostMapping(GatePilotApiPaths.AGENT_REGISTER)
    public Mono<ApiResponse<GatewayNode>> register(@Valid @RequestBody RegisterAgentCommand request) {
        // 注册只落节点资源，发布策略不在这里决策
        return Mono.just(ApiResponse.success(agentService.register(request), "节点注册成功"));
    }

    /**
     * 节点心跳。
     *
     * @param request 心跳请求
     * @return 节点资源
     */
    @PostMapping(GatePilotApiPaths.AGENT_HEARTBEAT)
    public Mono<ApiResponse<GatewayNode>> heartbeat(@Valid @RequestBody AgentHeartbeatCommand request) {
        // 心跳只更新节点状态，保持接口轻量
        return Mono.just(ApiResponse.success(agentService.heartbeat(request), "心跳已接收"));
    }

    /**
     * 拉取已发布配置。
     *
     * @param request 拉取请求
     * @return 已发布配置
     */
    @PostMapping(GatePilotApiPaths.AGENT_CONFIG_PULL)
    public Mono<ApiResponse<AgentConfigPullResult>> pullConfig(@Valid @RequestBody PullAgentConfigCommand request) {
        // agent 只拉已发布配置，不接触草稿资源
        return Mono.just(ApiResponse.success(agentService.pullConfig(request)));
    }

    /**
     * 上报配置应用结果。
     *
     * @param request 上报请求
     * @return 节点资源
     */
    @PostMapping(GatePilotApiPaths.AGENT_APPLY_RESULTS)
    public Mono<ApiResponse<GatewayNode>> reportApplyResult(
            @Valid @RequestBody ReportAgentApplyResultCommand request) {
        // apply 结果最终沉淀到节点状态
        return Mono.just(ApiResponse.success(agentService.reportApplyResult(request), "应用结果已接收"));
    }
}
