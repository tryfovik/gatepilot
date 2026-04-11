package com.dt.platform.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台网关配置属性。
 */
@ConfigurationProperties(prefix = "platform.gateway")
public class GatewayProperties {

    /**
     * 对外 API 前缀。
     */
    private String apiPrefix = "/api";

    /**
     * 内部运维前缀。
     */
    private String internalPrefix = "/internal";

    /**
     * 网关认证配置。
     */
    private AuthProperties auth = new AuthProperties();

    /**
     * 网关跨域配置。
     */
    private CorsProperties cors = new CorsProperties();

    /**
     * 上游健康检查配置。
     */
    private HealthProperties health = new HealthProperties();

    /**
     * 注入给上游的上下文头配置。
     */
    private ContextHeadersProperties contextHeaders = new ContextHeadersProperties();

    /**
     * 项目级路由配置。
     */
    private final Map<String, ProjectProperties> projects = new LinkedHashMap<>();

    public String getApiPrefix() {
        return apiPrefix;
    }

    public void setApiPrefix(String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    public String getInternalPrefix() {
        return internalPrefix;
    }

    public void setInternalPrefix(String internalPrefix) {
        this.internalPrefix = internalPrefix;
    }

    public AuthProperties getAuth() {
        return auth;
    }

    public void setAuth(AuthProperties auth) {
        this.auth = auth;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    public HealthProperties getHealth() {
        return health;
    }

    public void setHealth(HealthProperties health) {
        this.health = health;
    }

    public ContextHeadersProperties getContextHeaders() {
        return contextHeaders;
    }

    public void setContextHeaders(ContextHeadersProperties contextHeaders) {
        this.contextHeaders = contextHeaders;
    }

    public Map<String, ProjectProperties> getProjects() {
        return projects;
    }

    public void setProjects(Map<String, ProjectProperties> projects) {
        this.projects.clear();
        if (projects != null) {
            this.projects.putAll(projects);
        }
    }

    /**
     * 网关统一认证配置。
     */
    public static class AuthProperties {

        /**
         * 是否启用网关认证过滤。
         */
        private boolean enabled = true;

        /**
         * 是否跳过 OPTIONS 预检请求。
         */
        private boolean skipOptionsRequest = true;

        /**
         * 未登录时返回的 HTTP 状态码。
         */
        private int unauthorizedStatus = 401;

        /**
         * 未登录时返回的业务码。
         */
        private int unauthorizedCode = 401;

        /**
         * 未登录时返回的消息。
         */
        private String unauthorizedMessage = "Unauthorized";

        /**
         * 无权限时返回的 HTTP 状态码。
         */
        private int forbiddenStatus = 403;

        /**
         * 无权限时返回的业务码。
         */
        private int forbiddenCode = 403;

        /**
         * 无权限时返回的消息。
         */
        private String forbiddenMessage = "Forbidden";

        /**
         * 认证失败响应内容类型。
         */
        private String contentType = "application/json;charset=UTF-8";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isSkipOptionsRequest() {
            return skipOptionsRequest;
        }

        public void setSkipOptionsRequest(boolean skipOptionsRequest) {
            this.skipOptionsRequest = skipOptionsRequest;
        }

        public int getUnauthorizedStatus() {
            return unauthorizedStatus;
        }

        public void setUnauthorizedStatus(int unauthorizedStatus) {
            this.unauthorizedStatus = unauthorizedStatus;
        }

        public int getUnauthorizedCode() {
            return unauthorizedCode;
        }

        public void setUnauthorizedCode(int unauthorizedCode) {
            this.unauthorizedCode = unauthorizedCode;
        }

        public String getUnauthorizedMessage() {
            return unauthorizedMessage;
        }

        public void setUnauthorizedMessage(String unauthorizedMessage) {
            this.unauthorizedMessage = unauthorizedMessage;
        }

        public int getForbiddenStatus() {
            return forbiddenStatus;
        }

        public void setForbiddenStatus(int forbiddenStatus) {
            this.forbiddenStatus = forbiddenStatus;
        }

        public int getForbiddenCode() {
            return forbiddenCode;
        }

        public void setForbiddenCode(int forbiddenCode) {
            this.forbiddenCode = forbiddenCode;
        }

        public String getForbiddenMessage() {
            return forbiddenMessage;
        }

        public void setForbiddenMessage(String forbiddenMessage) {
            this.forbiddenMessage = forbiddenMessage;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    /**
     * 单个项目的路由配置。
     */
    public static class ProjectProperties {

        /**
         * 是否启用项目入口。
         */
        private boolean enabled = true;

        /**
         * 项目在网关入口上的路径分段。
         */
        private String pathSegment;

        /**
         * 项目展示名称。
         */
        private String displayName;

        /**
         * 项目下的路由配置。
         */
        private final Map<String, RouteProperties> routes = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPathSegment() {
            return pathSegment;
        }

        public void setPathSegment(String pathSegment) {
            this.pathSegment = pathSegment;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Map<String, RouteProperties> getRoutes() {
            return routes;
        }

        public void setRoutes(Map<String, RouteProperties> routes) {
            this.routes.clear();
            if (routes != null) {
                this.routes.putAll(routes);
            }
        }
    }

    /**
     * 单条路由配置。
     */
    public static class RouteProperties {

        /**
         * 是否启用当前路由。
         */
        private boolean enabled = true;

        /**
         * 是否启用 API 转发。
         */
        private boolean apiEnabled = true;

        /**
         * 是否启用内部运维转发。
         */
        private boolean actuatorEnabled = true;

        /**
         * 标准路径分段。
         */
        private String pathSegment;

        /**
         * API 上游地址。
         */
        private URI serviceUri;

        /**
         * API 允许的方法列表。
         */
        private List<String> apiMethods = new ArrayList<>();

        /**
         * API 最大请求体大小。
         */
        private DataSize apiMaxRequestSize;

        /**
         * API 上游路径前缀。
         */
        private String servicePathPrefix;

        /**
         * Actuator 上游地址。
         */
        private URI actuatorUri;

        /**
         * 内部运维入口允许的方法列表。
         */
        private List<String> internalMethods = new ArrayList<>();

        /**
         * 内部运维最大请求体大小。
         */
        private DataSize internalMaxRequestSize;

        /**
         * 响应超时时间。
         */
        private Duration responseTimeout;

        /**
         * 连接超时时间，单位毫秒。
         */
        private Integer connectTimeoutMs;

        /**
         * 认证策略。
         */
        private AuthRuleProperties auth = new AuthRuleProperties();

        /**
         * 路由治理策略。
         */
        private GovernanceProperties governance = new GovernanceProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isApiEnabled() {
            return apiEnabled;
        }

        public void setApiEnabled(boolean apiEnabled) {
            this.apiEnabled = apiEnabled;
        }

        public boolean isActuatorEnabled() {
            return actuatorEnabled;
        }

        public void setActuatorEnabled(boolean actuatorEnabled) {
            this.actuatorEnabled = actuatorEnabled;
        }

        public String getPathSegment() {
            return pathSegment;
        }

        public void setPathSegment(String pathSegment) {
            this.pathSegment = pathSegment;
        }

        public URI getServiceUri() {
            return serviceUri;
        }

        public void setServiceUri(URI serviceUri) {
            this.serviceUri = serviceUri;
        }

        public List<String> getApiMethods() {
            return apiMethods;
        }

        public void setApiMethods(List<String> apiMethods) {
            this.apiMethods = apiMethods == null ? new ArrayList<>() : new ArrayList<>(apiMethods);
        }

        public DataSize getApiMaxRequestSize() {
            return apiMaxRequestSize;
        }

        public void setApiMaxRequestSize(DataSize apiMaxRequestSize) {
            this.apiMaxRequestSize = apiMaxRequestSize;
        }

        public String getServicePathPrefix() {
            return servicePathPrefix;
        }

        public void setServicePathPrefix(String servicePathPrefix) {
            this.servicePathPrefix = servicePathPrefix;
        }

        public URI getActuatorUri() {
            return actuatorUri;
        }

        public void setActuatorUri(URI actuatorUri) {
            this.actuatorUri = actuatorUri;
        }

        public List<String> getInternalMethods() {
            return internalMethods;
        }

        public void setInternalMethods(List<String> internalMethods) {
            this.internalMethods = internalMethods == null ? new ArrayList<>() : new ArrayList<>(internalMethods);
        }

        public DataSize getInternalMaxRequestSize() {
            return internalMaxRequestSize;
        }

        public void setInternalMaxRequestSize(DataSize internalMaxRequestSize) {
            this.internalMaxRequestSize = internalMaxRequestSize;
        }

        public Duration getResponseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
        }

        public Integer getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(Integer connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public AuthRuleProperties getAuth() {
            return auth;
        }

        public void setAuth(AuthRuleProperties auth) {
            this.auth = auth;
        }

        public GovernanceProperties getGovernance() {
            return governance;
        }

        public void setGovernance(GovernanceProperties governance) {
            this.governance = governance;
        }
    }

    /**
     * 单条路由的认证规则。
     */
    public static class AuthRuleProperties {

        /**
         * 当前路由是否要求认证。
         */
        private boolean required;

        /**
         * 路由内可匿名访问的相对路径。
         */
        private List<String> publicPaths = new ArrayList<>();

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public List<String> getPublicPaths() {
            return publicPaths;
        }

        public void setPublicPaths(List<String> publicPaths) {
            this.publicPaths = publicPaths;
        }
    }

    /**
     * 单条路由的治理规则。
     */
    public static class GovernanceProperties {

        /**
         * Sentinel 流控规则。
         */
        private FlowControlProperties flowControl = new FlowControlProperties();

        public FlowControlProperties getFlowControl() {
            return flowControl;
        }

        public void setFlowControl(FlowControlProperties flowControl) {
            this.flowControl = flowControl;
        }
    }

    /**
     * 路由级流控配置。
     */
    public static class FlowControlProperties {

        /**
         * 是否启用当前路由的 Sentinel 流控。
         */
        private boolean enabled;

        /**
         * 允许通过的 QPS 阈值。
         */
        private Double count;

        /**
         * 统计窗口秒数。
         */
        private long intervalSec = 1;

        /**
         * 突发流量额度。
         */
        private int burst;

        /**
         * Sentinel 控制行为。
         */
        private String controlBehavior = "default";

        /**
         * 排队等待超时时间，单位毫秒。
         */
        private int maxQueueingTimeoutMs;

        /**
         * 参数热点限流配置。
         */
        private FlowControlParamProperties param = new FlowControlParamProperties();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Double getCount() {
            return count;
        }

        public void setCount(Double count) {
            this.count = count;
        }

        public long getIntervalSec() {
            return intervalSec;
        }

        public void setIntervalSec(long intervalSec) {
            this.intervalSec = intervalSec;
        }

        public int getBurst() {
            return burst;
        }

        public void setBurst(int burst) {
            this.burst = burst;
        }

        public String getControlBehavior() {
            return controlBehavior;
        }

        public void setControlBehavior(String controlBehavior) {
            this.controlBehavior = controlBehavior;
        }

        public int getMaxQueueingTimeoutMs() {
            return maxQueueingTimeoutMs;
        }

        public void setMaxQueueingTimeoutMs(int maxQueueingTimeoutMs) {
            this.maxQueueingTimeoutMs = maxQueueingTimeoutMs;
        }

        public FlowControlParamProperties getParam() {
            return param;
        }

        public void setParam(FlowControlParamProperties param) {
            this.param = param;
        }
    }

    /**
     * 热点参数流控配置。
     */
    public static class FlowControlParamProperties {

        /**
         * 是否启用参数维度流控。
         */
        private boolean enabled;

        /**
         * 参数解析策略。
         */
        private String parseStrategy;

        /**
         * 头、Cookie 或 URL 参数名称。
         */
        private String fieldName;

        /**
         * 参数值匹配模式。
         */
        private String pattern;

        /**
         * 参数匹配策略。
         */
        private String matchStrategy = "exact";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getParseStrategy() {
            return parseStrategy;
        }

        public void setParseStrategy(String parseStrategy) {
            this.parseStrategy = parseStrategy;
        }

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String getMatchStrategy() {
            return matchStrategy;
        }

        public void setMatchStrategy(String matchStrategy) {
            this.matchStrategy = matchStrategy;
        }
    }

    /**
     * 上游健康检查配置。
     */
    public static class HealthProperties {

        /**
         * 健康检查路径。
         */
        private String path = "/actuator/health";

        /**
         * 健康检查超时时间。
         */
        private Duration timeout = Duration.ofSeconds(3);

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * 网关跨域配置。
     */
    public static class CorsProperties {

        /**
         * 是否启用网关跨域处理。
         */
        private boolean enabled = true;

        /**
         * 允许的来源模式。
         */
        private List<String> allowedOriginPatterns = new ArrayList<>(List.of("http://127.0.0.1:*", "http://localhost:*"));

        /**
         * 允许的请求方法。
         */
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        /**
         * 允许的请求头。
         */
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));

        /**
         * 允许前端读取的响应头。
         */
        private List<String> exposedHeaders = new ArrayList<>(List.of("X-Trace-Id", "Authorization"));

        /**
         * 是否允许携带凭证。
         */
        private boolean allowCredentials = true;

        /**
         * 预检结果缓存时间。
         */
        private Duration maxAge = Duration.ofHours(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * 注入到上游的上下文头配置。
     */
    public static class ContextHeadersProperties {

        /**
         * 是否启用平台上下文头。
         */
        private boolean enabled = true;

        /**
         * 项目标识头名称。
         */
        private String projectHeaderName = "X-Platform-Project";

        /**
         * 路由标识头名称。
         */
        private String routeHeaderName = "X-Platform-Route";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProjectHeaderName() {
            return projectHeaderName;
        }

        public void setProjectHeaderName(String projectHeaderName) {
            this.projectHeaderName = projectHeaderName;
        }

        public String getRouteHeaderName() {
            return routeHeaderName;
        }

        public void setRouteHeaderName(String routeHeaderName) {
            this.routeHeaderName = routeHeaderName;
        }
    }
}
