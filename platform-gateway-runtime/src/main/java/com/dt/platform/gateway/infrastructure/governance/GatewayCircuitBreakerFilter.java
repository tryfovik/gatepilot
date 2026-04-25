package com.dt.platform.gateway.infrastructure.governance;

import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinition;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 路由级轻量熔断过滤器。
 */
public class GatewayCircuitBreakerFilter implements WebFilter {

    /**
     * 当前请求是否命中了熔断 fallback。
     */
    public static final String FALLBACK_ATTRIBUTE =
            GatewayCircuitBreakerFilter.class.getName() + ".fallback";

    private final GatewayRouteDefinitionLocator routeDefinitionLocator;

    private final DispatcherHandler dispatcherHandler;

    private final ConcurrentMap<String, CircuitState> states = new ConcurrentHashMap<>();

    /**
     * 创建熔断过滤器。
     *
     * @param routeDefinitionLocator 路由定义定位器
     * @param dispatcherHandler WebFlux 分发器
     */
    public GatewayCircuitBreakerFilter(GatewayRouteDefinitionLocator routeDefinitionLocator,
                                       DispatcherHandler dispatcherHandler) {
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.dispatcherHandler = dispatcherHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Optional<GatewayRouteDefinition> routeDefinition = routeDefinitionLocator.findApiRoute(
                exchange.getRequest().getPath().value()
        );
        if (routeDefinition.isEmpty() || routeDefinition.get().getCircuitBreakerPolicy() == null) {
            return chain.filter(exchange);
        }
        GatewayRouteDefinition.CircuitBreakerPolicy policy = routeDefinition.get().getCircuitBreakerPolicy();
        CircuitState state = states.computeIfAbsent(policy.name(), ignored -> new CircuitState());
        long startNanos = System.nanoTime();
        if (!state.tryAcquire(policy, startNanos)) {
            return fallback(exchange, policy);
        }
        return chain.filter(exchange)
                .doOnSuccess(ignored -> state.record(
                        policy,
                        isFailureStatus(exchange, policy),
                        isSlowCall(policy, startNanos),
                        System.nanoTime()
                ))
                .onErrorResume(exception -> {
                    state.record(policy, true, isSlowCall(policy, startNanos), System.nanoTime());
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.error(exception);
                    }
                    return fallback(exchange, policy);
                });
    }

    private boolean isFailureStatus(ServerWebExchange exchange,
                                    GatewayRouteDefinition.CircuitBreakerPolicy policy) {
        if (exchange.getResponse().getStatusCode() == null) {
            return false;
        }
        return policy.statusCodes().contains(exchange.getResponse().getStatusCode().value());
    }

    private boolean isSlowCall(GatewayRouteDefinition.CircuitBreakerPolicy policy, long startNanos) {
        Duration threshold = policy.slowCallDurationThreshold();
        return threshold != null && System.nanoTime() - startNanos >= threshold.toNanos();
    }

    private Mono<Void> fallback(ServerWebExchange exchange,
                                GatewayRouteDefinition.CircuitBreakerPolicy policy) {
        exchange.getAttributes().put(FALLBACK_ATTRIBUTE, true);
        if (policy.fallbackUri() != null && "forward".equalsIgnoreCase(policy.fallbackUri().getScheme())) {
            return forwardFallback(exchange, policy.fallbackUri());
        }
        return writeFallbackJson(exchange, policy);
    }

    private Mono<Void> forwardFallback(ServerWebExchange exchange, URI fallbackUri) {
        String target = fallbackUri.getSchemeSpecificPart();
        if (!StringUtils.hasText(target)) {
            return Mono.error(new IllegalArgumentException("gateway circuit breaker fallback uri path must not be blank"));
        }
        int queryIndex = target.indexOf('?');
        String path = queryIndex < 0 ? target : target.substring(0, queryIndex);
        String query = queryIndex < 0 ? null : target.substring(queryIndex + 1);
        URI uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                .replacePath(path)
                .replaceQuery(query)
                .build(true)
                .toUri();
        ServerHttpRequest request = exchange.getRequest().mutate()
                .uri(uri)
                .path(path)
                .build();
        return dispatcherHandler.handle(exchange.mutate().request(request).build());
    }

    private Mono<Void> writeFallbackJson(ServerWebExchange exchange,
                                         GatewayRouteDefinition.CircuitBreakerPolicy policy) {
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(policy.fallbackStatus()));
        exchange.getResponse().getHeaders().setContentType(MediaType.parseMediaType(policy.contentType()));
        byte[] body = ("{\"status\":\"fail\",\"code\":" + policy.fallbackCode()
                + ",\"message\":\"" + escapeJson(policy.fallbackMessage()) + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    }
                    else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static final class CircuitState {

        private CircuitStatus status = CircuitStatus.CLOSED;

        private long openUntilNanos;

        private int halfOpenActiveCalls;

        private int halfOpenCompletedCalls;

        private final Deque<CallOutcome> outcomes = new ArrayDeque<>();

        private synchronized boolean tryAcquire(GatewayRouteDefinition.CircuitBreakerPolicy policy, long nowNanos) {
            if (status == CircuitStatus.CLOSED) {
                return true;
            }
            if (status == CircuitStatus.OPEN && nowNanos >= openUntilNanos) {
                status = CircuitStatus.HALF_OPEN;
                halfOpenActiveCalls = 0;
                halfOpenCompletedCalls = 0;
            }
            if (status == CircuitStatus.HALF_OPEN
                    && halfOpenActiveCalls < policy.permittedNumberOfCallsInHalfOpenState()) {
                halfOpenActiveCalls++;
                return true;
            }
            return false;
        }

        private synchronized void record(GatewayRouteDefinition.CircuitBreakerPolicy policy,
                                         boolean failure,
                                         boolean slow,
                                         long nowNanos) {
            if (status == CircuitStatus.HALF_OPEN) {
                halfOpenActiveCalls = Math.max(0, halfOpenActiveCalls - 1);
                halfOpenCompletedCalls++;
                if (failure || slow) {
                    open(policy, nowNanos);
                    return;
                }
                if (halfOpenCompletedCalls >= policy.permittedNumberOfCallsInHalfOpenState()) {
                    close();
                }
                return;
            }
            if (status != CircuitStatus.CLOSED) {
                return;
            }
            outcomes.addLast(new CallOutcome(failure, slow));
            while (outcomes.size() > policy.slidingWindowSize()) {
                outcomes.removeFirst();
            }
            if (outcomes.size() < policy.minimumNumberOfCalls()) {
                return;
            }
            int failures = 0;
            int slowCalls = 0;
            for (CallOutcome outcome : outcomes) {
                if (outcome.failure()) {
                    failures++;
                }
                if (outcome.slow()) {
                    slowCalls++;
                }
            }
            double failureRate = failures * 100D / outcomes.size();
            double slowCallRate = slowCalls * 100D / outcomes.size();
            if (failureRate >= policy.failureRateThreshold()
                    || (policy.slowCallDurationThreshold() != null
                    && slowCallRate >= policy.slowCallRateThreshold())) {
                open(policy, nowNanos);
            }
        }

        private void open(GatewayRouteDefinition.CircuitBreakerPolicy policy, long nowNanos) {
            status = CircuitStatus.OPEN;
            openUntilNanos = nowNanos + policy.waitDurationInOpenState().toNanos();
            halfOpenActiveCalls = 0;
            halfOpenCompletedCalls = 0;
        }

        private void close() {
            status = CircuitStatus.CLOSED;
            outcomes.clear();
            halfOpenActiveCalls = 0;
            halfOpenCompletedCalls = 0;
        }
    }

    private enum CircuitStatus {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private record CallOutcome(boolean failure, boolean slow) {
    }
}
