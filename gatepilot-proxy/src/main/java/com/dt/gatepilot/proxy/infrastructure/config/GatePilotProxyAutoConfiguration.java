package com.dt.gatepilot.proxy.infrastructure.config;

import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthChecker;
import com.dt.gatepilot.proxy.domain.port.RuntimeGovernanceRulePublisher;
import com.dt.gatepilot.proxy.domain.port.RuntimeMetricsSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CircuitBreakerPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.ProxyUpstreamHealthConstants;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.ReleaseUpstreamResolver;
import com.dt.gatepilot.proxy.domain.runtime.RetryPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteCircuitBreaker;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import com.dt.gatepilot.proxy.domain.runtime.UpstreamEndpointHealthRegistry;
import com.dt.gatepilot.proxy.infrastructure.auth.GetbootRuntimeAuthChecker;
import com.dt.gatepilot.proxy.infrastructure.health.UpstreamHealthProbe;
import com.dt.gatepilot.proxy.infrastructure.health.UpstreamHealthProbeScheduler;
import com.dt.gatepilot.proxy.infrastructure.governance.ProxySentinelConstants;
import com.dt.gatepilot.proxy.infrastructure.governance.SentinelGatewayRulePublisher;
import com.dt.gatepilot.proxy.infrastructure.limiter.GetbootRuntimeRateLimiter;
import com.dt.gatepilot.proxy.infrastructure.loadbalancer.GatePilotLoadBalancerClientConfiguration;
import com.dt.gatepilot.proxy.infrastructure.loadbalancer.GatePilotLoadBalancerConstants;
import com.dt.gatepilot.proxy.infrastructure.metrics.MicrometerRuntimeMetricsSink;
import com.dt.gatepilot.proxy.interfaces.control.ProxyRuntimeControlController;
import com.dt.gatepilot.proxy.interfaces.gateway.GatePilotGatewayFilter;
import com.dt.gatepilot.proxy.interfaces.gateway.GatePilotRouteLocator;
import com.dt.gatepilot.proxy.interfaces.web.ProxyRuntimeAuditRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.auth.spi.SaTokenWebFluxAuthChecker;
import com.getboot.limiter.api.registry.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GatePilot proxy 自动配置。
 */
@AutoConfiguration
@ConditionalOnGatePilotProxyEnabled
@ConditionalOnClass(RouteLocator.class)
@EnableScheduling
public class GatePilotProxyAutoConfiguration {

    /**
     * 创建 proxy 运行态。
     *
     * @return proxy 运行态
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyRuntimeState proxyRuntimeState() {
        // 默认每个进程只有一份本地运行态
        return new ProxyRuntimeState();
    }

    /**
     * 创建 PublishedConfig 编译器。
     *
     * @return PublishedConfig 编译器
     */
    @Bean
    @ConditionalOnMissingBean
    public PublishedConfigCompiler publishedConfigCompiler() {
        // 编译器无状态，可以直接复用单例
        return new PublishedConfigCompiler();
    }

    /**
     * 创建 proxy 配置应用器。
     *
     * @param compiler PublishedConfig 编译器
     * @param runtimeState proxy 运行态
     * @param governanceRulePublisherProvider 治理规则发布端口提供器
     * @return proxy 配置应用器
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyConfigApplier proxyConfigApplier(PublishedConfigCompiler compiler,
                                                 ProxyRuntimeState runtimeState,
                                                 ObjectProvider<RuntimeGovernanceRulePublisher>
                                                         governanceRulePublisherProvider) {
        // apply 负责把新配置编译后切到运行态
        return new ProxyConfigApplier(compiler, runtimeState,
                governanceRulePublisherProvider.getIfAvailable(() -> runtime -> {
                }));
    }

    /**
     * 创建 proxy runtime control API
     *
     * @param proxyConfigApplier proxy 配置应用器
     * @return proxy runtime control API
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyRuntimeControlController proxyRuntimeControlController(ProxyConfigApplier proxyConfigApplier) {
        // 只接收 agent 的配置应用请求，不提供管理配置能力
        return new ProxyRuntimeControlController(proxyConfigApplier);
    }

    /**
     * 创建 Sentinel 网关规则发布器。
     *
     * @param rateLimitPolicyResolver 限流策略解析器
     * @return Sentinel 网关规则发布器
     */
    @Bean
    @ConditionalOnMissingBean(RuntimeGovernanceRulePublisher.class)
    @ConditionalOnClass(name = {
            "com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager",
            "com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager"
    })
    @ConditionalOnProperty(prefix = ProxySentinelConstants.GETBOOT_GOVERNANCE_PREFIX,
            name = {ProxySentinelConstants.ENABLED_PROPERTY, ProxySentinelConstants.SENTINEL_ENABLED_PROPERTY},
            havingValue = ProxySentinelConstants.ENABLED_VALUE)
    public RuntimeGovernanceRulePublisher sentinelRuntimeGovernanceRulePublisher(
            RateLimitPolicyResolver rateLimitPolicyResolver) {
        // Sentinel 接入开关和配置统一走 getboot-governance
        return new SentinelGatewayRulePublisher(rateLimitPolicyResolver);
    }

    /**
     * 创建路由访问判断器。
     *
     * @return 路由访问判断器
     */
    @Bean
    @ConditionalOnMissingBean
    public RouteAccessEvaluator routeAccessEvaluator() {
        // 访问判断保持纯内存计算，不访问控制面
        return new RouteAccessEvaluator();
    }

    /**
     * 创建流量染色解析器。
     *
     * @return 流量染色解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public TrafficColorResolver trafficColorResolver() {
        // 染色解析只消费已编译策略快照
        return new TrafficColorResolver();
    }

    /**
     * 创建熔断策略解析器。
     *
     * @return 熔断策略解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerPolicyResolver circuitBreakerPolicyResolver() {
        // 熔断策略解析只消费已编译策略快照
        return new CircuitBreakerPolicyResolver();
    }

    /**
     * 创建路由熔断器。
     *
     * @return 路由熔断器
     */
    @Bean
    @ConditionalOnMissingBean
    public RouteCircuitBreaker routeCircuitBreaker() {
        // 熔断状态按 proxy 进程本地维护，扩副本时天然隔离
        return new RouteCircuitBreaker();
    }

    /**
     * 创建限流策略解析器。
     *
     * @return 限流策略解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitPolicyResolver rateLimitPolicyResolver() {
        // 限流策略解析只消费已编译策略快照
        return new RateLimitPolicyResolver();
    }

    /**
     * 创建重试策略解析器。
     *
     * @return 重试策略解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public RetryPolicyResolver retryPolicyResolver() {
        // 重试策略解析只消费已编译策略快照
        return new RetryPolicyResolver();
    }

    /**
     * 创建发布上游解析器。
     *
     * @return 发布上游解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public ReleaseUpstreamResolver releaseUpstreamResolver() {
        // 发布策略只影响上游选择，不改变路由命中规则
        return new ReleaseUpstreamResolver();
    }

    /**
     * 创建上游端点健康状态表。
     *
     * @return 上游端点健康状态表
     */
    @Bean
    @ConditionalOnMissingBean
    public UpstreamEndpointHealthRegistry upstreamEndpointHealthRegistry() {
        // 健康状态只在本 proxy 进程内生效
        return new UpstreamEndpointHealthRegistry();
    }

    /**
     * 创建上游健康探测器。
     *
     * @param runtimeState proxy 运行态
     * @param healthRegistry 端点健康状态表
     * @return 上游健康探测器
     */
    @Bean
    @ConditionalOnMissingBean
    public UpstreamHealthProbe upstreamHealthProbe(ProxyRuntimeState runtimeState,
                                                   UpstreamEndpointHealthRegistry healthRegistry) {
        // 健康探测必须绕开 LoadBalancer，否则 IP 会被当成服务名
        return new UpstreamHealthProbe(runtimeState, healthRegistry, WebClient.builder().build());
    }

    /**
     * 创建上游健康探测调度器。
     *
     * @param probe 上游健康探测器
     * @return 上游健康探测调度器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ProxyUpstreamHealthConstants.CONFIG_PREFIX,
            name = ProxyUpstreamHealthConstants.ENABLED_PROPERTY,
            havingValue = ProxyUpstreamHealthConstants.ENABLED_VALUE,
            matchIfMissing = true)
    public UpstreamHealthProbeScheduler upstreamHealthProbeScheduler(UpstreamHealthProbe probe) {
        // 调度器只触发本地探测，不访问控制面
        return new UpstreamHealthProbeScheduler(probe);
    }

    /**
     * 创建 GatePilot LoadBalancer 默认配置。
     *
     * @return LoadBalancer 默认配置
     */
    @Bean
    @ConditionalOnMissingBean(name = GatePilotLoadBalancerConstants.CLIENT_SPECIFICATION_BEAN_NAME)
    public LoadBalancerClientSpecification gatePilotLoadBalancerClientSpecification() {
        // 所有 GatePilot upstream 都使用同一套 LoadBalancer 适配
        return new LoadBalancerClientSpecification(
                GatePilotLoadBalancerConstants.DEFAULT_SPECIFICATION_NAME,
                new Class<?>[]{GatePilotLoadBalancerClientConfiguration.class}
        );
    }

    /**
     * 创建运行时限流器。
     *
     * @param registryProvider getboot 限流注册表提供器
     * @return 运行时限流器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeRateLimiter runtimeRateLimiter(ObjectProvider<RateLimiterRegistry> registryProvider) {
        // 具体限流算法交给 getboot-limiter，proxy 只做策略适配
        return new GetbootRuntimeRateLimiter(registryProvider);
    }

    /**
     * 创建运行时认证校验器。
     *
     * @param checkerProvider getboot-auth 认证校验器提供器
     * @return 运行时认证校验器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeAuthChecker runtimeAuthChecker(ObjectProvider<SaTokenWebFluxAuthChecker> checkerProvider) {
        // 认证执行交给 getboot-auth，proxy 只消费结果
        return new GetbootRuntimeAuthChecker(checkerProvider);
    }

    /**
     * 创建默认运行审计采集器。
     *
     * @return 运行审计采集器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeAuditSink runtimeAuditSink() {
        // 默认不落地，agent 接入后替换为上报实现
        return event -> {
        };
    }

    /**
     * 创建 Micrometer 运行指标采集器。
     *
     * @param meterRegistry Micrometer 注册表
     * @return 运行指标采集器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public RuntimeMetricsSink micrometerRuntimeMetricsSink(MeterRegistry meterRegistry) {
        // 指标出口交给 getboot-observability 配置和暴露
        return new MicrometerRuntimeMetricsSink(meterRegistry);
    }

    /**
     * 创建默认运行指标采集器。
     *
     * @return 运行指标采集器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeMetricsSink runtimeMetricsSink() {
        // 没有 MeterRegistry 时保持空实现，避免影响 proxy 启动
        return (routeId, status, latencyMillis) -> {
        };
    }

    /**
     * 创建 proxy 运行审计记录器。
     *
     * @param runtimeAuditSink 运行审计采集器
     * @param runtimeMetricsSink 运行指标采集器
     * @return proxy 运行审计记录器
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyRuntimeAuditRecorder proxyRuntimeAuditRecorder(RuntimeAuditSink runtimeAuditSink,
                                                               RuntimeMetricsSink runtimeMetricsSink) {
        // 记录器只负责组装和派发，不在 proxy 内保存审计数据
        return new ProxyRuntimeAuditRecorder(runtimeAuditSink, runtimeMetricsSink);
    }

    /**
     * 创建 GatePilot SCG 入口路由。
     *
     * @return SCG 路由定位器
     */
    @Bean
    @ConditionalOnMissingBean(name = "gatePilotRouteLocator")
    public RouteLocator gatePilotRouteLocator() {
        // 只注册数据面兜底入口，真实上游由运行态过滤器选择
        return new GatePilotRouteLocator();
    }

    /**
     * 创建 GatePilot SCG 治理过滤器。
     *
     * @param runtimeState proxy 运行态
     * @param accessEvaluator 路由访问判断器
     * @param trafficColorResolver 流量染色解析器
     * @param circuitBreakerPolicyResolver 熔断策略解析器
     * @param routeCircuitBreaker 路由熔断器
     * @param rateLimitPolicyResolver 限流策略解析器
     * @param runtimeRateLimiter 运行时限流器
     * @param retryPolicyResolver 重试策略解析器
     * @param releaseUpstreamResolver 发布上游解析器
     * @param runtimeAuthChecker 运行时认证校验器
     * @param auditRecorder 运行审计记录器
     * @param retryFactoryProvider SCG 重试过滤器工厂提供器
     * @param objectMapper JSON 序列化器
     * @return GatePilot SCG 治理过滤器
     */
    @Bean
    @ConditionalOnMissingBean
    public GatePilotGatewayFilter gatePilotGatewayFilter(ProxyRuntimeState runtimeState,
                                                         RouteAccessEvaluator accessEvaluator,
                                                         TrafficColorResolver trafficColorResolver,
                                                         CircuitBreakerPolicyResolver circuitBreakerPolicyResolver,
                                                         RouteCircuitBreaker routeCircuitBreaker,
                                                         RateLimitPolicyResolver rateLimitPolicyResolver,
                                                         RuntimeRateLimiter runtimeRateLimiter,
                                                         RetryPolicyResolver retryPolicyResolver,
                                                         ReleaseUpstreamResolver releaseUpstreamResolver,
                                                         RuntimeAuthChecker runtimeAuthChecker,
                                                         ProxyRuntimeAuditRecorder auditRecorder,
                                                         ObjectProvider<RetryGatewayFilterFactory> retryFactoryProvider,
                                                         ObjectMapper objectMapper) {
        // 转发交给 SCG，GatePilot 只做运行态决策和请求改写
        return new GatePilotGatewayFilter(
                runtimeState,
                accessEvaluator,
                trafficColorResolver,
                circuitBreakerPolicyResolver,
                routeCircuitBreaker,
                rateLimitPolicyResolver,
                runtimeRateLimiter,
                retryPolicyResolver,
                releaseUpstreamResolver,
                runtimeAuthChecker,
                auditRecorder,
                retryFactoryProvider.getIfAvailable(RetryGatewayFilterFactory::new),
                objectMapper
        );
    }
}
