package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyConstants;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyRequest;
import com.dt.gatepilot.proxy.application.dto.ProxyApplyResult;
import com.dt.gatepilot.proxy.domain.port.RuntimeGovernanceRulePublisher;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import java.time.Instant;

/**
 * proxy 配置应用器，负责编译 PublishedConfig 并原子切换运行态。
 */
public class ProxyConfigApplier {

    private final PublishedConfigCompiler compiler;

    private final ProxyRuntimeState runtimeState;

    private final RuntimeGovernanceRulePublisher governanceRulePublisher;

    private final UpstreamDiscoveryRegistry upstreamDiscoveryRegistry;

    /**
     * 创建配置应用器。
     */
    public ProxyConfigApplier() {
        this(new PublishedConfigCompiler(), new ProxyRuntimeState(), runtime -> {
        }, new StaticUpstreamDiscoveryRegistry());
    }

    /**
     * 创建配置应用器。
     *
     * @param compiler 配置编译器
     * @param runtimeState 运行态
     */
    public ProxyConfigApplier(PublishedConfigCompiler compiler, ProxyRuntimeState runtimeState) {
        this(compiler, runtimeState, runtime -> {
        }, new StaticUpstreamDiscoveryRegistry());
    }

    /**
     * 创建配置应用器。
     *
     * @param compiler 配置编译器
     * @param runtimeState 运行态
     * @param governanceRulePublisher 治理规则发布端口
     */
    public ProxyConfigApplier(PublishedConfigCompiler compiler,
                              ProxyRuntimeState runtimeState,
                              RuntimeGovernanceRulePublisher governanceRulePublisher) {
        this(compiler, runtimeState, governanceRulePublisher, new StaticUpstreamDiscoveryRegistry());
    }

    /**
     * 创建配置应用器。
     *
     * @param compiler 配置编译器
     * @param runtimeState 运行态
     * @param governanceRulePublisher 治理规则发布端口
     * @param upstreamDiscoveryRegistry 上游服务发现注册表
     */
    public ProxyConfigApplier(PublishedConfigCompiler compiler,
                              ProxyRuntimeState runtimeState,
                              RuntimeGovernanceRulePublisher governanceRulePublisher,
                              UpstreamDiscoveryRegistry upstreamDiscoveryRegistry) {
        this.compiler = compiler;
        this.runtimeState = runtimeState;
        this.governanceRulePublisher = governanceRulePublisher;
        this.upstreamDiscoveryRegistry = upstreamDiscoveryRegistry;
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
        CompiledProxyRuntime compiledRuntime;
        try {
            compiledRuntime = compiler.compile(config);
        } catch (RuntimeException exception) {
            // 编译异常需要回传给 agent，再由 agent 上报控制面
            return ProxyApplyResult.failed(version, configHash, ProxyApplyConstants.REASON_COMPILE_FAILED,
                    exception.getMessage(), startedAt);
        }
        if (!request.isDryRun()) {
            try {
                // 先发布治理规则，成功后才切换本地运行态
                governanceRulePublisher.publish(compiledRuntime);
            } catch (RuntimeException exception) {
                return ProxyApplyResult.failed(version, configHash,
                        ProxyApplyConstants.REASON_GOVERNANCE_RULE_PUBLISH_FAILED,
                        exception.getMessage(), startedAt);
            }
            runtimeState.switchTo(compiledRuntime);
            upstreamDiscoveryRegistry.refresh(compiledRuntime);
        }
        return ProxyApplyResult.applied(version, configHash, startedAt);
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
