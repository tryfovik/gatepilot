package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
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
        // ANY 表示不限制 HTTP 方法
        if (route.getMethods().stream().anyMatch(methodValue -> HttpMethod.ANY == methodValue)) {
            return true;
        }
        String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
        return route.getMethods().stream().map(Enum::name).anyMatch(normalizedMethod::equals);
    }

    private List<String> allowedMethods(CompiledRoute route) {
        // 没有明确限制时不返回 Allow 头
        if (route.getMethods().stream().anyMatch(method -> HttpMethod.ANY == method)) {
            return List.of();
        }
        return route.getMethods().stream().map(Enum::name).toList();
    }

    private boolean authenticationRequired(CompiledProxyRuntime runtime, CompiledRoute route, String requestPath) {
        if (route.isPoliciesPrecompiled()) {
            return authenticationRequired(route.getAuthPolicies() == null ? List.of() : route.getAuthPolicies(),
                    requestPath);
        }
        if (route.getAuthPolicies() != null) {
            return authenticationRequired(route.getAuthPolicies(), requestPath);
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_AUTH)) {
            if (publicPath(route, policy, requestPath)) {
                continue;
            }
            if (requiresAuthentication(policy)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 预编译路由认证策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 预编译认证策略
     */
    public List<CompiledAuthPolicy> compile(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (runtime == null || route == null) {
            return List.of();
        }
        List<CompiledAuthPolicy> compiledPolicies = new ArrayList<>();
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_AUTH)) {
            compiledPolicies.add(new CompiledAuthPolicy(requiresAuthentication(policy),
                    publicPathPrefixes(route, policy)));
        }
        return List.copyOf(compiledPolicies);
    }

    private boolean authenticationRequired(List<CompiledAuthPolicy> policies, String requestPath) {
        String normalizedRequestPath = normalizePath(requestPath);
        for (CompiledAuthPolicy policy : policies) {
            if (policy.publicPath(normalizedRequestPath)) {
                continue;
            }
            if (policy.requiresAuthentication()) {
                return true;
            }
        }
        return false;
    }

    private boolean requiresAuthentication(CompiledPolicy policy) {
        if (Boolean.TRUE.equals(booleanValue(policy.getConfig().get(
                PublishedConfigConstants.KEY_ANONYMOUS_ALLOWED)))) {
            return false;
        }
        return !AuthType.NONE.name().equalsIgnoreCase(Objects.toString(
                policy.getConfig().get(PublishedConfigConstants.KEY_TYPE), ""));
    }

    private boolean publicPath(CompiledRoute route, CompiledPolicy policy, String requestPath) {
        Object publicPaths = policy.getConfig().get(PublishedConfigConstants.KEY_PUBLIC_PATHS);
        if (!(publicPaths instanceof Iterable<?> iterable) || requestPath == null) {
            return false;
        }
        String normalizedRequestPath = normalizePath(requestPath);
        String routePrefix = normalizePath(route.getPathPrefix());
        for (Object item : iterable) {
            // publicPath 是相对路由前缀的白名单路径
            String publicPath = normalizeRelativePath(Objects.toString(item, null));
            if (publicPath == null) {
                continue;
            }
            String fullPath = ProxyPathConstants.ROOT_PATH.equals(publicPath) ? routePrefix : routePrefix + publicPath;
            if (normalizedRequestPath.equals(fullPath)
                    || normalizedRequestPath.startsWith(fullPath + ProxyPathConstants.PATH_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }

    private List<String> publicPathPrefixes(CompiledRoute route, CompiledPolicy policy) {
        Object publicPaths = policy.getConfig().get(PublishedConfigConstants.KEY_PUBLIC_PATHS);
        if (!(publicPaths instanceof Iterable<?> iterable)) {
            return List.of();
        }
        String routePrefix = normalizePath(route.getPathPrefix());
        if (routePrefix == null) {
            return List.of();
        }
        List<String> prefixes = new ArrayList<>();
        for (Object item : iterable) {
            // 编译阶段就把相对路径转成完整前缀
            String publicPath = normalizeRelativePath(Objects.toString(item, null));
            if (publicPath != null) {
                prefixes.add(ProxyPathConstants.ROOT_PATH.equals(publicPath) ? routePrefix : routePrefix + publicPath);
            }
        }
        return List.copyOf(prefixes);
    }

    private List<CompiledPolicy> policiesByType(CompiledProxyRuntime runtime, CompiledRoute route, String type) {
        List<CompiledPolicy> policies = new ArrayList<>();
        for (String policyName : route.getPolicyNames()) {
            // 路由只保存策略名，运行态通过索引取策略
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
        // JSON 载荷里可能出现字符串形式的布尔值
        return value == null ? null : Boolean.valueOf(value.toString());
    }

    private String normalizeRelativePath(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        // 白名单路径统一去掉末尾斜杠
        while (normalized.length() > 1 && normalized.endsWith(ProxyPathConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        // 路径比较只看 path，不带 query
        String normalized = path.startsWith(ProxyPathConstants.PATH_SEPARATOR)
                ? path
                : ProxyPathConstants.PATH_SEPARATOR + path;
        int queryIndex = normalized.indexOf(ProxyPathConstants.QUERY_SEPARATOR);
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith(ProxyPathConstants.PATH_SEPARATOR)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
