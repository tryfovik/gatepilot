package com.dt.gatepilot.demo.interfaces.rest;

import com.dt.gatepilot.demo.application.dto.DemoEchoResponse;
import com.dt.gatepilot.demo.application.service.DemoEchoService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 示例上游回显 API
 */
@RestController
public class DemoUpstreamController {

    /**
     * 回显服务
     */
    private final DemoEchoService echoService;

    /**
     * 创建示例上游控制器
     *
     * @param echoService 回显服务
     */
    public DemoUpstreamController(DemoEchoService echoService) {
        this.echoService = echoService;
    }

    /**
     * 回显所有业务请求
     *
     * @param request 上游收到的请求
     * @return 回显响应
     */
    @RequestMapping(DemoHttpConstants.ALL_PATHS)
    public Mono<DemoEchoResponse> echo(ServerHttpRequest request) {
        // demo 只负责暴露转发效果，不做业务判断
        return Mono.just(echoService.echo(request));
    }
}
