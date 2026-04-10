package com.dt.platform.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关路由配置属性。
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
     * 平台统一上游配置。
     */
    private final Map<String, UpstreamProperties> upstreams = new LinkedHashMap<>();

    /**
     * 上游健康检查配置。
     */
    private HealthProperties health = new HealthProperties();

    /**
     * 跨域配置。
     */
    private CorsProperties cors = new CorsProperties();

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

    public Map<String, UpstreamProperties> getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(Map<String, UpstreamProperties> upstreams) {
        this.upstreams.clear();
        if (upstreams != null) {
            this.upstreams.putAll(upstreams);
        }
    }

    public HealthProperties getHealth() {
        return health;
    }

    public void setHealth(HealthProperties health) {
        this.health = health;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        this.cors = cors;
    }

    /**
     * 单个上游应用的路由配置。
     */
    public static class UpstreamProperties {

        /**
         * 是否启用当前上游。
         */
        private boolean enabled = true;

        /**
         * 是否启用对外 API 路由。
         */
        private boolean apiEnabled = true;

        /**
         * 是否启用内部运维路由。
         */
        private boolean actuatorEnabled = true;

        /**
         * 路由分段名称。
         */
        private String routeSegment;

        /**
         * API 转发目标地址。
         */
        private URI serviceUri;

        /**
         * API 转发目标路径前缀。
         */
        private String servicePathPrefix;

        /**
         * Actuator 转发与健康探测目标地址。
         */
        private URI actuatorUri;

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

        public String getRouteSegment() {
            return routeSegment;
        }

        public void setRouteSegment(String routeSegment) {
            this.routeSegment = routeSegment;
        }

        public URI getServiceUri() {
            return serviceUri;
        }

        public void setServiceUri(URI serviceUri) {
            this.serviceUri = serviceUri;
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
}
