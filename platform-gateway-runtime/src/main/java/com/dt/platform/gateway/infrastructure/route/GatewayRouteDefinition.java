package com.dt.platform.gateway.infrastructure.route;

import org.springframework.http.server.PathContainer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 运行期标准化后的网关路由定义。
 */
public final class GatewayRouteDefinition {

    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    private final String projectKey;

    private final String projectSegment;

    private final String routeKey;

    private final String routeSegment;

    private final List<String> apiPathRoots;

    private final List<String> internalPathRoots;

    private final URI serviceUri;

    private final List<String> apiMethods;

    private final Set<String> apiMethodSet;

    private final DataSize apiMaxRequestSize;

    private final String servicePathPrefix;

    private final URI actuatorUri;

    private final List<String> internalMethods;

    private final Set<String> internalMethodSet;

    private final DataSize internalMaxRequestSize;

    private final boolean authRequired;

    private final List<String> publicApiPaths;

    private final List<PathPattern> publicApiPatterns;

    private final Integer connectTimeoutMs;

    private final Duration responseTimeout;

    GatewayRouteDefinition(String projectKey,
                           String projectSegment,
                           String routeKey,
                           String routeSegment,
                           List<String> apiPathRoots,
                           List<String> internalPathRoots,
                           URI serviceUri,
                           List<String> apiMethods,
                           DataSize apiMaxRequestSize,
                           String servicePathPrefix,
                           URI actuatorUri,
                           List<String> internalMethods,
                           DataSize internalMaxRequestSize,
                           boolean authRequired,
                           List<String> publicApiPatterns,
                           Integer connectTimeoutMs,
                           Duration responseTimeout) {
        this.projectKey = projectKey;
        this.projectSegment = projectSegment;
        this.routeKey = routeKey;
        this.routeSegment = routeSegment;
        this.apiPathRoots = List.copyOf(apiPathRoots);
        this.internalPathRoots = List.copyOf(internalPathRoots);
        this.serviceUri = serviceUri;
        this.apiMethods = sanitizeHttpMethods(apiMethods);
        this.apiMethodSet = Set.copyOf(this.apiMethods);
        this.apiMaxRequestSize = apiMaxRequestSize;
        this.servicePathPrefix = servicePathPrefix;
        this.actuatorUri = actuatorUri;
        this.internalMethods = sanitizeHttpMethods(internalMethods);
        this.internalMethodSet = Set.copyOf(this.internalMethods);
        this.internalMaxRequestSize = internalMaxRequestSize;
        this.authRequired = authRequired;
        this.publicApiPaths = List.copyOf(publicApiPatterns);
        this.publicApiPatterns = this.publicApiPaths.stream()
                .map(this::normalizeRelativePathPattern)
                .map(PATH_PATTERN_PARSER::parse)
                .toList();
        this.connectTimeoutMs = connectTimeoutMs;
        this.responseTimeout = responseTimeout;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getProjectSegment() {
        return projectSegment;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public String getRouteSegment() {
        return routeSegment;
    }

    public List<String> getApiPathRoots() {
        return apiPathRoots;
    }

    public List<String> getInternalPathRoots() {
        return internalPathRoots;
    }

    public URI getServiceUri() {
        return serviceUri;
    }

    public List<String> getApiMethods() {
        return apiMethods;
    }

    public DataSize getApiMaxRequestSize() {
        return apiMaxRequestSize;
    }

    public String getServicePathPrefix() {
        return servicePathPrefix;
    }

    public URI getActuatorUri() {
        return actuatorUri;
    }

    public List<String> getInternalMethods() {
        return internalMethods;
    }

    public DataSize getInternalMaxRequestSize() {
        return internalMaxRequestSize;
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    public List<String> getPublicApiPaths() {
        return publicApiPaths;
    }

    public Integer getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public Duration getResponseTimeout() {
        return responseTimeout;
    }

    /**
     * 判断当前请求方法是否允许访问 API 路由。
     *
     * @param requestMethod 请求方法
     * @return 是否允许
     */
    public boolean isApiMethodAllowed(String requestMethod) {
        return isMethodAllowed(apiMethods, apiMethodSet, requestMethod);
    }

    /**
     * 判断当前请求方法是否允许访问内部运维路由。
     *
     * @param requestMethod 请求方法
     * @return 是否允许
     */
    public boolean isInternalMethodAllowed(String requestMethod) {
        return isMethodAllowed(internalMethods, internalMethodSet, requestMethod);
    }

    /**
     * 判断请求是否命中 API 路由。
     *
     * @param requestPath 请求路径
     * @return 是否命中
     */
    public boolean matchesApiPath(String requestPath) {
        return findMatchedApiRoot(requestPath).isPresent();
    }

    /**
     * 判断请求是否命中内部运维路由。
     *
     * @param requestPath 请求路径
     * @return 是否命中
     */
    public boolean matchesInternalPath(String requestPath) {
        return findMatchedInternalRoot(requestPath).isPresent();
    }

    /**
     * 判断请求是否是路由内公开接口。
     *
     * @param requestPath 请求路径
     * @return 是否公开
     */
    public boolean isPublicApiPath(String requestPath) {
        Optional<String> relativePath = resolveRelativeApiPath(requestPath);
        if (relativePath.isEmpty()) {
            return false;
        }
        PathContainer path = PathContainer.parsePath(relativePath.get());
        return publicApiPatterns.stream().anyMatch(pattern -> pattern.matches(path));
    }

    /**
     * 查找命中的 API 根路径。
     *
     * @param requestPath 请求路径
     * @return 命中的根路径
     */
    public Optional<String> findMatchedApiRoot(String requestPath) {
        return apiPathRoots.stream()
                .filter(root -> matchesPathRoot(requestPath, root))
                .findFirst();
    }

    /**
     * 查找命中的内部根路径。
     *
     * @param requestPath 请求路径
     * @return 命中的根路径
     */
    public Optional<String> findMatchedInternalRoot(String requestPath) {
        return internalPathRoots.stream()
                .filter(root -> matchesPathRoot(requestPath, root))
                .findFirst();
    }

    /**
     * 解析 API 请求相对路径。
     *
     * @param requestPath 请求路径
     * @return 相对路径
     */
    public Optional<String> resolveRelativeApiPath(String requestPath) {
        return findMatchedApiRoot(requestPath).map(root -> resolveRelativePath(root, requestPath));
    }

    /**
     * 生成用于注入到上游的上下文头。
     *
     * @param projectHeaderName 项目标识头
     * @param routeHeaderName 路由标识头
     * @return 上下文头
     */
    public Map<String, String> buildContextHeaders(String projectHeaderName, String routeHeaderName) {
        return Map.of(
                projectHeaderName, projectKey,
                routeHeaderName, routeKey
        );
    }

    /**
     * 构造稳定的路由标识。
     *
     * @param routeType 路由类型
     * @param routePathRoot 路由根路径
     * @return 路由标识
     */
    public String buildRouteId(String routeType, String routePathRoot) {
        return sanitize(projectKey) + "-" + sanitize(routeKey) + "-" + routeType + "-" + sanitize(routePathRoot);
    }

    /**
     * 构造当前路由在治理系统中的 API 分组资源名。
     *
     * @return 治理 API 分组资源名
     */
    public String buildGovernanceApiName() {
        return "platform-gateway-api-" + sanitize(projectKey) + "-" + sanitize(routeKey);
    }

    private boolean matchesPathRoot(String requestPath, String pathRoot) {
        return requestPath.equals(pathRoot) || requestPath.startsWith(pathRoot + "/");
    }

    private String resolveRelativePath(String pathRoot, String requestPath) {
        if (requestPath.equals(pathRoot)) {
            return "/";
        }
        return requestPath.substring(pathRoot.length());
    }

    private String normalizeRelativePathPattern(String pathPattern) {
        if (pathPattern == null || pathPattern.isBlank()) {
            return "/";
        }
        return pathPattern.startsWith("/") ? pathPattern : "/" + pathPattern;
    }

    private List<String> sanitizeHttpMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        for (String method : methods) {
            String normalized = normalizeHttpMethod(method);
            if (normalized != null) {
                sanitized.add(normalized);
            }
        }
        return List.copyOf(sanitized);
    }

    private boolean isMethodAllowed(List<String> allowedMethods,
                                    Set<String> allowedMethodSet,
                                    String requestMethod) {
        if (allowedMethods.isEmpty()) {
            return true;
        }
        String normalizedMethod = normalizeHttpMethod(requestMethod);
        return normalizedMethod != null && allowedMethodSet.contains(normalizedMethod);
    }

    private String normalizeHttpMethod(String requestMethod) {
        if (requestMethod == null || requestMethod.isBlank()) {
            return null;
        }
        return requestMethod.trim().toUpperCase(Locale.ROOT);
    }

    private String sanitize(String value) {
        return value == null ? "route" : value.replaceAll("[^a-zA-Z0-9-]", "-");
    }
}
