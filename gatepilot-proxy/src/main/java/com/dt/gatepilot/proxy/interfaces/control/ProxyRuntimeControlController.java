package com.dt.gatepilot.proxy.interfaces.control;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.infrastructure.config.ConditionalOnGatePilotProxyEnabled;
import com.getboot.web.api.response.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * proxy runtime control API
 */
@RestController
@ConditionalOnGatePilotProxyEnabled
@RequestMapping(ProxyRuntimeApiPaths.PROXY)
public class ProxyRuntimeControlController {

    private final ProxyConfigApplier proxyConfigApplier;

    /**
     * 创建 proxy runtime control API
     *
     * @param proxyConfigApplier proxy 配置应用器
     */
    public ProxyRuntimeControlController(ProxyConfigApplier proxyConfigApplier) {
        this.proxyConfigApplier = proxyConfigApplier;
    }

    /**
     * 应用 agent 下发的 PublishedConfig
     *
     * @param publishedConfig 已发布配置
     * @return proxy 应用结果
     */
    @PostMapping(ProxyRuntimeApiPaths.APPLY_CONFIG)
    public Mono<ApiResponse<ProxyApplyResult>> applyConfig(@RequestBody PublishedConfig publishedConfig) {
        ProxyApplyRequest request = new ProxyApplyRequest();
        // controller 只做协议转换，真正 apply 仍在 proxy runtime
        request.setPublishedConfig(publishedConfig);
        return Mono.just(ApiResponse.success(proxyConfigApplier.apply(request),
                ProxyRuntimeApiPaths.MESSAGE_APPLY_CONFIG_FINISHED));
    }
}
