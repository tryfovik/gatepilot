package com.dt.gatepilot.proxy.infrastructure.config;

import com.dt.gatepilot.proxy.domain.port.RuntimeAuditSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeMetricsSink;
import com.dt.gatepilot.proxy.domain.port.RuntimeRateLimiter;
import com.dt.gatepilot.proxy.domain.runtime.CircuitBreakerPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.PublishedConfigCompiler;
import com.dt.gatepilot.proxy.domain.runtime.RateLimitPolicyResolver;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteCircuitBreaker;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import com.dt.gatepilot.proxy.infrastructure.limiter.GetbootRuntimeRateLimiter;
import com.dt.gatepilot.proxy.interfaces.web.GatePilotProxyHandler;
import com.dt.gatepilot.proxy.interfaces.web.ProxyHttpConstants;
import com.dt.gatepilot.proxy.interfaces.web.ProxyRuntimeAuditRecorder;
import com.getboot.limiter.api.registry.RateLimiterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * GatePilot proxy 自动配置。
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
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
     * @return proxy 配置应用器
     */
    @Bean
    @ConditionalOnMissingBean
    public ProxyConfigApplier proxyConfigApplier(PublishedConfigCompiler compiler, ProxyRuntimeState runtimeState) {
        // apply 负责把新配置编译后切到运行态
        return new ProxyConfigApplier(compiler, runtimeState);
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
     * 创建默认运行指标采集器。
     *
     * @return 运行指标采集器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeMetricsSink runtimeMetricsSink() {
        // 默认不落地，后续接 Micrometer 或 agent 上报实现
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
     * 创建 proxy WebFlux 入口处理器。
     *
     * @param runtimeState proxy 运行态
     * @param accessEvaluator 路由访问判断器
     * @param trafficColorResolver 流量染色解析器
     * @param circuitBreakerPolicyResolver 熔断策略解析器
     * @param routeCircuitBreaker 路由熔断器
     * @param rateLimitPolicyResolver 限流策略解析器
     * @param runtimeRateLimiter 运行时限流器
     * @param auditRecorder 运行审计记录器
     * @param webClientBuilder WebClient 构造器
     * @return proxy WebFlux 入口处理器
     */
    @Bean
    @ConditionalOnMissingBean
    public GatePilotProxyHandler gatePilotProxyHandler(ProxyRuntimeState runtimeState,
                                                       RouteAccessEvaluator accessEvaluator,
                                                       TrafficColorResolver trafficColorResolver,
                                                       CircuitBreakerPolicyResolver circuitBreakerPolicyResolver,
                                                       RouteCircuitBreaker routeCircuitBreaker,
                                                       RateLimitPolicyResolver rateLimitPolicyResolver,
                                                       RuntimeRateLimiter runtimeRateLimiter,
                                                       ProxyRuntimeAuditRecorder auditRecorder,
                                                       WebClient.Builder webClientBuilder) {
        // WebClient.Builder 由 getboot-http-client 增强时可自动继承 Trace 透传
        return new GatePilotProxyHandler(
                runtimeState,
                accessEvaluator,
                trafficColorResolver,
                circuitBreakerPolicyResolver,
                routeCircuitBreaker,
                rateLimitPolicyResolver,
                runtimeRateLimiter,
                auditRecorder,
                webClientBuilder.build()
        );
    }

    /**
     * 创建 proxy WebFlux 路由。
     *
     * @param handler proxy WebFlux 入口处理器
     * @return WebFlux 路由
     */
    @Bean
    public RouterFunction<ServerResponse> gatePilotProxyRoutes(GatePilotProxyHandler handler) {
        // proxy 入口只吃业务路径，管理 API 留给 apiserver
        return RouterFunctions.route(proxyPath(), handler::handle);
    }

    /**
     * 创建 proxy 路由匹配器。
     *
     * @return proxy 路由匹配器
     */
    private RequestPredicate proxyPath() {
        // GatePilot 管理 API 不走数据面代理
        return RequestPredicates.path(ProxyHttpConstants.API_PROXY_PATH)
                .or(RequestPredicates.path(ProxyHttpConstants.INTERNAL_PROXY_PATH))
                .and(request -> !request.path().startsWith(ProxyHttpConstants.GATEPILOT_API_PREFIX));
    }
}
