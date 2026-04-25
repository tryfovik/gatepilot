package com.dt.gatepilot.proxy.support.runtime;

import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.api.ProxyApplyRequest;
import com.dt.gatepilot.proxy.api.ProxyApplyResult;
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
            return ProxyApplyResult.failed(null, null, "EmptyConfig", "PublishedConfig 不能为空", startedAt);
        }
        String version = config.getSpec().getVersion();
        String configHash = config.getSpec().getConfigHash();
        if (!hasText(version)) {
            return ProxyApplyResult.failed(version, configHash, "MissingVersion", "PublishedConfig 缺少版本号", startedAt);
        }
        if (!hasText(configHash)) {
            return ProxyApplyResult.failed(version, configHash, "MissingConfigHash", "PublishedConfig 缺少配置哈希", startedAt);
        }
        try {
            CompiledProxyRuntime compiledRuntime = compiler.compile(config);
            if (!request.isDryRun()) {
                runtimeState.switchTo(compiledRuntime);
            }
            return ProxyApplyResult.applied(version, configHash, startedAt);
        } catch (RuntimeException exception) {
            return ProxyApplyResult.failed(version, configHash, "CompileFailed", exception.getMessage(), startedAt);
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
        return value != null && !value.trim().isEmpty();
    }
}
