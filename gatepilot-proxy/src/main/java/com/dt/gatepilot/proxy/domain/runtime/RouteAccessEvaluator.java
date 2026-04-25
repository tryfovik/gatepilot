package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.AuthType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 基于已编译运行态判断路由访问策略。
 */
public class RouteAccessEvaluator {

    /**
     * 判断一次请求的路由访问策略。
     *
     * @param runtime 已编译运行态
     * @param request 访问请求
     * @return 判断结果
     */
    public RouteAccessDecision evaluate(CompiledProxyRuntime runtime, RouteAccessRequest request) {
        if (runtime == null || request == null) {
            return RouteAccessDecision.notMatched();
        }
        CompiledRoute route = runtime.match(request.host(), request.path());
        if (route == null) {
            return RouteAccessDecision.notMatched();
        }
        List<String> allowedMethods = allowedMethods(route);
        boolean methodAllowed = methodAllowed(route, request.method());
        boolean authenticationRequired = methodAllowed && authenticationRequired(runtime, route, request.path());
        return new RouteAccessDecision(true, route, methodAllowed, allowedMethods, authenticationRequired);
    }

    private boolean methodAllowed(CompiledRoute route, String method) {
        if (route.getMethods().isEmpty() || method == null || method.isBlank()) {
            return true;
        }
        if (route.getMethods().stream().anyMatch(methodValue -> "ANY".equals(methodValue.name()))) {
            return true;
        }
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        return route.getMethods().stream().map(Enum::name).anyMatch(normalizedMethod::equals);
    }

    private List<String> allowedMethods(CompiledRoute route) {
        if (route.getMethods().stream().anyMatch(method -> "ANY".equals(method.name()))) {
            return List.of();
        }
        return route.getMethods().stream().map(Enum::name).toList();
    }

    private boolean authenticationRequired(CompiledProxyRuntime runtime, CompiledRoute route, String requestPath) {
        for (CompiledPolicy policy : policiesByType(runtime, route, "AuthPolicy")) {
            if (publicPath(route, policy, requestPath)) {
                continue;
            }
            if (Boolean.TRUE.equals(booleanValue(policy.getConfig().get("anonymousAllowed")))) {
                continue;
            }
            if (AuthType.NONE.name().equalsIgnoreCase(Objects.toString(policy.getConfig().get("type"), ""))) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean publicPath(CompiledRoute route, CompiledPolicy policy, String requestPath) {
        Object publicPaths = policy.getConfig().get("publicPaths");
        if (!(publicPaths instanceof Iterable<?> iterable) || requestPath == null) {
            return false;
        }
        String normalizedRequestPath = normalizePath(requestPath);
        String routePrefix = normalizePath(route.getPathPrefix());
        for (Object item : iterable) {
            String publicPath = normalizeRelativePath(Objects.toString(item, null));
            if (publicPath == null) {
                continue;
            }
            String fullPath = "/".equals(publicPath) ? routePrefix : routePrefix + publicPath;
            if (normalizedRequestPath.equals(fullPath) || normalizedRequestPath.startsWith(fullPath + "/")) {
                return true;
            }
        }
        return false;
    }

    private List<CompiledPolicy> policiesByType(CompiledProxyRuntime runtime, CompiledRoute route, String type) {
        List<CompiledPolicy> policies = new ArrayList<>();
        for (String policyName : route.getPolicyNames()) {
            CompiledPolicy policy = runtime.getPoliciesByName().get(policyName);
            if (policy != null && type.equalsIgnoreCase(Objects.toString(policy.getType(), ""))) {
                policies.add(policy);
            }
        }
        return policies;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private String normalizeRelativePath(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
