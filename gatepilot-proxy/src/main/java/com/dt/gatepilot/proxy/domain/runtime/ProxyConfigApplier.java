package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyConstants;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import java.time.Instant;

/**
 * proxy 配置应用器，负责编译 PublishedConfig 并原子切换运行态。
 */
public class ProxyConfigApplier {

    private final PublishedConfigCompiler compiler;

    private final ProxyRuntimeState runtimeState;

    /**
     * 创建配置应用器。
     */
    public ProxyConfigApplier() {
        this(new PublishedConfigCompiler(), new ProxyRuntimeState());
    }

    /**
     * 创建配置应用器。
     *
     * @param compiler 配置编译器
     * @param runtimeState 运行态
     */
    public ProxyConfigApplier(PublishedConfigCompiler compiler, ProxyRuntimeState runtimeState) {
        this.compiler = compiler;
        this.runtimeState = runtimeState;
    }

    /**
     * 应用配置。
     *
     * @param request 应用请求
     * @return 应用结果
     */
    public ProxyApplyResult apply(ProxyApplyRequest request) {
        Instant startedAt = Instant.now();
        PublishedConfig config = request.getPublishedConfig();
        if (config == null) {
            // 空配置直接失败，不能影响当前运行态
            return ProxyApplyResult.failed(null, null, ProxyApplyConstants.REASON_EMPTY_CONFIG,
                    ProxyApplyConstants.MESSAGE_EMPTY_CONFIG, startedAt);
        }
        String version = config.getSpec().getVersion();
        String configHash = config.getSpec().getConfigHash();
        if (!hasText(version)) {
            // 版本是 agent 上报和排障的主键
            return ProxyApplyResult.failed(version, configHash, ProxyApplyConstants.REASON_MISSING_VERSION,
                    ProxyApplyConstants.MESSAGE_MISSING_VERSION, startedAt);
        }
        if (!hasText(configHash)) {
            // hash 缺失说明发布产物不完整
            return ProxyApplyResult.failed(version, configHash, ProxyApplyConstants.REASON_MISSING_CONFIG_HASH,
                    ProxyApplyConstants.MESSAGE_MISSING_CONFIG_HASH, startedAt);
        }
        try {
            CompiledProxyRuntime compiledRuntime = compiler.compile(config);
            if (!request.isDryRun()) {
                // 编译成功后才原子切换，失败不影响旧运行态
                runtimeState.switchTo(compiledRuntime);
            }
            return ProxyApplyResult.applied(version, configHash, startedAt);
        } catch (RuntimeException exception) {
            // 编译异常需要回传给 agent，再由 agent 上报控制面
            return ProxyApplyResult.failed(version, configHash, ProxyApplyConstants.REASON_COMPILE_FAILED,
                    exception.getMessage(), startedAt);
        }
    }

    /**
     * 查询运行态。
     *
     * @return 运行态
     */
    public ProxyRuntimeState runtimeState() {
        return runtimeState;
    }

    private boolean hasText(String value) {
        // 这里不引 Spring 工具类，保持 domain 轻量
        return value != null && !value.trim().isEmpty();
    }
}
