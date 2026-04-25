package com.dt.gatepilot.proxy.interfaces.web;

import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledRoute;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import com.dt.gatepilot.proxy.domain.runtime.ProxyRuntimeState;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessDecision;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessEvaluator;
import com.dt.gatepilot.proxy.domain.runtime.RouteAccessRequest;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorRequest;
import com.dt.gatepilot.proxy.domain.runtime.TrafficColorResolver;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * GatePilot proxy WebFlux 入口处理器。
 */
public class GatePilotProxyHandler {

    private static final String TRAFFIC_COLOR_HEADER = "X-Traffic-Color";

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "host",
            "content-length"
    );

    private final ProxyRuntimeState runtimeState;

    private final RouteAccessEvaluator accessEvaluator;

    private final TrafficColorResolver trafficColorResolver;

    private final WebClient webClient;

    /**
     * 创建 proxy WebFlux 入口处理器。
     *
     * @param runtimeState proxy 运行态
     * @param accessEvaluator 路由访问判断器
     * @param trafficColorResolver 流量染色解析器
     * @param webClient WebClient
     */
    public GatePilotProxyHandler(ProxyRuntimeState runtimeState,
                                 RouteAccessEvaluator accessEvaluator,
                                 TrafficColorResolver trafficColorResolver,
                                 WebClient webClient) {
        this.runtimeState = runtimeState;
        this.accessEvaluator = accessEvaluator;
        this.trafficColorResolver = trafficColorResolver;
        this.webClient = webClient;
    }

    /**
     * 处理一次业务转发请求。
     *
     * @param request WebFlux 请求
     * @return WebFlux 响应
     */
    public Mono<ServerResponse> handle(ServerRequest request) {
        // 没有运行态就先拒绝，避免空配置接流量
        return runtimeState.current()
                .map(runtime -> handleWithRuntime(runtime, request))
                .orElseGet(() -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    private Mono<ServerResponse> handleWithRuntime(CompiledProxyRuntime runtime, ServerRequest request) {
        // 热路径先做路由和访问判断，后面只处理已命中的路由
        RouteAccessDecision accessDecision = accessEvaluator.evaluate(runtime, routeAccessRequest(request));
        if (!accessDecision.matched()) {
            return ServerResponse.notFound().build();
        }
        if (!accessDecision.methodAllowed()) {
            return methodNotAllowed(accessDecision);
        }
        if (accessDecision.authenticationRequired()) {
            // getboot-auth 还没接入前，先按策略拒绝需要认证的请求
            return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
        }
        CompiledUpstream upstream = runtime.getUpstreamsByName().get(accessDecision.route().getUpstreamName());
        if (upstream == null || upstream.getEndpoints().isEmpty()) {
            // 上游缺失说明发布产物不可用，不能继续转发
            return ServerResponse.status(HttpStatus.BAD_GATEWAY).build();
        }
        // 染色结果会同时写到上游请求和客户端响应
        String trafficColor = trafficColorResolver.resolve(runtime, trafficColorRequest(request));
        URI targetUri = targetUri(request, accessDecision.route(), upstream);
        return forward(request, accessDecision.route(), targetUri, trafficColor);
    }

    private RouteAccessRequest routeAccessRequest(ServerRequest request) {
        // 访问判断只需要路由命中、路径和方法
        return new RouteAccessRequest(host(request), request.uri().getRawPath(), request.methodName());
    }

    private TrafficColorRequest trafficColorRequest(ServerRequest request) {
        // 用读取器懒取值，避免为了染色提前解析整包请求
        return new TrafficColorRequest(
                host(request),
                request.uri().getRawPath(),
                request.uri().getRawQuery(),
                headerName -> request.headers().firstHeader(headerName),
                cookieName -> request.cookies().getFirst(cookieName) == null
                        ? null
                        : request.cookies().getFirst(cookieName).getValue(),
                queryName -> request.queryParam(queryName).orElse(null),
                remoteAddress(request)
        );
    }

    private Mono<ServerResponse> methodNotAllowed(RouteAccessDecision decision) {
        return ServerResponse.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers -> {
                    // 只有明确方法白名单时才回 Allow
                    if (!decision.allowedMethods().isEmpty()) {
                        headers.setAllow(new HashSet<>(decision.allowedMethods().stream()
                                .map(org.springframework.http.HttpMethod::valueOf)
                                .toList()));
                    }
                })
                .build();
    }

    private Mono<ServerResponse> forward(ServerRequest request,
                                         CompiledRoute route,
                                         URI targetUri,
                                         String trafficColor) {
        // 请求体保持流式透传，避免网关把大包读进内存
        Flux<DataBuffer> body = request.bodyToFlux(DataBuffer.class);
        return webClient.method(request.method())
                .uri(targetUri)
                .headers(headers -> applyRequestHeaders(request, route, headers, trafficColor))
                .body(BodyInserters.fromDataBuffers(body))
                .exchangeToMono(response -> toServerResponse(response, trafficColor));
    }

    private Mono<ServerResponse> toServerResponse(ClientResponse response, String trafficColor) {
        return ServerResponse.status(response.statusCode())
                .headers(headers -> {
                    // 先复制上游响应头，再补回网关计算出的染色结果
                    copyResponseHeaders(response, headers);
                    if (StringUtils.hasText(trafficColor)) {
                        headers.set(TRAFFIC_COLOR_HEADER, trafficColor);
                    }
                })
                .body(BodyInserters.fromDataBuffers(response.bodyToFlux(DataBuffer.class)));
    }

    private void applyRequestHeaders(ServerRequest request,
                                     CompiledRoute route,
                                     HttpHeaders targetHeaders,
                                     String trafficColor) {
        // 客户端请求头先按代理规则复制，再套路由级改写
        request.headers().asHttpHeaders().forEach((name, values) -> {
            if (!skipRequestHeader(route, name)) {
                targetHeaders.addAll(name, values);
            }
        });
        route.getAddHeaders().forEach(targetHeaders::set);
        if (StringUtils.hasText(trafficColor)) {
            targetHeaders.set(TRAFFIC_COLOR_HEADER, trafficColor);
        }
    }

    private void copyResponseHeaders(ClientResponse response, HttpHeaders targetHeaders) {
        // hop-by-hop 响应头不能跨代理继续传
        response.headers().asHttpHeaders().forEach((name, values) -> {
            if (!hopByHop(name)) {
                targetHeaders.addAll(name, values);
            }
        });
    }

    private boolean skipRequestHeader(CompiledRoute route, String name) {
        // hop-by-hop 和路由声明移除的头都不透传
        if (hopByHop(name)) {
            return true;
        }
        return route.getRemoveHeaders().stream()
                .filter(Objects::nonNull)
                .anyMatch(header -> header.equalsIgnoreCase(name));
    }

    private boolean hopByHop(String name) {
        // HTTP 头名大小写不敏感，统一按小写比较
        return name == null || HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }

    private URI targetUri(ServerRequest request, CompiledRoute route, CompiledUpstream upstream) {
        // 当前先取第一个端点，负载均衡后续独立补
        CompiledUpstream.CompiledEndpoint endpoint = upstream.getEndpoints().get(0);
        String path = targetPath(request.uri().getRawPath(), route);
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(scheme(upstream))
                .host(endpoint.getHost())
                .path(path);
        if (endpoint.getPort() != null) {
            builder.port(endpoint.getPort());
        }
        if (StringUtils.hasText(request.uri().getRawQuery())) {
            builder.query(request.uri().getRawQuery());
        }
        return builder.build(true).toUri();
    }

    private String targetPath(String requestPath, CompiledRoute route) {
        // rewrite 比 stripPrefix 更明确，优先使用 rewrite
        String suffix = suffix(requestPath, route.getPathPrefix());
        if (StringUtils.hasText(route.getRewritePathPrefix())) {
            return joinPath(route.getRewritePathPrefix(), suffix);
        }
        if (Boolean.TRUE.equals(route.getStripPrefix())) {
            return suffix.isEmpty() ? "/" : suffix;
        }
        return requestPath;
    }

    private String suffix(String requestPath, String prefix) {
        // 理论上这里已经命中过路由，兜底仍然保持空后缀
        if (requestPath == null || prefix == null || !requestPath.startsWith(prefix)) {
            return "";
        }
        String suffix = requestPath.substring(prefix.length());
        return suffix.isEmpty() ? "" : (suffix.startsWith("/") ? suffix : "/" + suffix);
    }

    private String joinPath(String prefix, String suffix) {
        // 保证 prefix 和 suffix 中间只有一个斜杠
        String normalizedPrefix = prefix.endsWith("/") && prefix.length() > 1
                ? prefix.substring(0, prefix.length() - 1)
                : prefix;
        if (!StringUtils.hasText(suffix) || "/".equals(suffix)) {
            return normalizedPrefix;
        }
        return normalizedPrefix + (suffix.startsWith("/") ? suffix : "/" + suffix);
    }

    private String scheme(CompiledUpstream upstream) {
        // WebSocket 先按 HTTP/S 转发入口处理
        Protocol protocol = upstream.getProtocol();
        if (protocol == Protocol.HTTPS || protocol == Protocol.WSS) {
            return "https";
        }
        return "http";
    }

    private String host(ServerRequest request) {
        // Host 用于运行态路由索引
        return request.headers().firstHeader(HttpHeaders.HOST);
    }

    private String remoteAddress(ServerRequest request) {
        // IP 染色和后续审计都会复用这个地址
        return request.remoteAddress()
                .map(this::remoteAddress)
                .orElse(null);
    }

    private String remoteAddress(InetSocketAddress address) {
        // InetAddress 为空时保留原始 host
        return address.getAddress() == null ? address.getHostString() : address.getAddress().getHostAddress();
    }
}
