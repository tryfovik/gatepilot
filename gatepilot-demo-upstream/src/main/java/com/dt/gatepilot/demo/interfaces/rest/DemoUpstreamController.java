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
