package com.dt.platform.gateway.infrastructure.validation;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 网关配置启动校验器。
 */
public class GatewayPropertiesValidator {

    /**
     * 创建网关配置校验器。
     *
     * @param properties 网关配置
     */
    public GatewayPropertiesValidator(GatewayProperties properties) {
        validatePrefixes(properties);
        validateUpstreams(properties);
        validateRouteSegments(properties);
        validateInternalPrefix(properties);
        validateCors(properties);
    }

    /**
     * 校验前缀配置。
     *
     * @param properties 网关配置
     */
    private void validatePrefixes(GatewayProperties properties) {
        String apiPrefix = normalizePrefix(properties.getApiPrefix());
        String internalPrefix = normalizePrefix(properties.getInternalPrefix());
        if (StringUtils.hasText(apiPrefix) && apiPrefix.equals(internalPrefix)) {
            throw new IllegalArgumentException("gateway api prefix and internal prefix must not be identical");
        }
    }

    /**
     * 校验全部上游配置。
     *
     * @param properties 网关配置
     */
    private void validateUpstreams(GatewayProperties properties) {
        if (properties.getUpstreams().isEmpty()) {
            throw new IllegalArgumentException("gateway upstreams must not be empty");
        }
        boolean hasEnabledUpstream = false;
        for (Map.Entry<String, GatewayProperties.UpstreamProperties> entry : properties.getUpstreams().entrySet()) {
            GatewayProperties.UpstreamProperties upstream = entry.getValue();
            if (!upstream.isEnabled()) {
                continue;
            }
            hasEnabledUpstream = true;
            validateUpstream(entry.getKey(), upstream);
        }
        if (!hasEnabledUpstream) {
            throw new IllegalArgumentException("at least one gateway upstream must be enabled");
        }
    }

    /**
     * 校验单个上游配置。
     *
     * @param upstreamName 上游名称
     * @param upstream 上游配置
     */
    private void validateUpstream(String upstreamName, GatewayProperties.UpstreamProperties upstream) {
        if (upstream.isApiEnabled()) {
            requireText(upstream.getRouteSegment(),
                    "gateway route segment must not be blank when api route is enabled: " + upstreamName);
            requireUri(upstream.getServiceUri(),
                    "gateway service uri must not be null when api route is enabled: " + upstreamName);
        }
        if (upstream.isActuatorEnabled()) {
            requireText(upstream.getRouteSegment(),
                    "gateway route segment must not be blank when actuator route is enabled: " + upstreamName);
            requireUri(upstream.getActuatorUri(),
                    "gateway actuator uri must not be null when actuator route is enabled: " + upstreamName);
        }
    }

    /**
     * 校验路由分段唯一性。
     *
     * @param properties 网关配置
     */
    private void validateRouteSegments(GatewayProperties properties) {
        Map<String, String> routeSegments = new LinkedHashMap<>();
        for (Map.Entry<String, GatewayProperties.UpstreamProperties> entry : properties.getUpstreams().entrySet()) {
            registerRouteSegment(routeSegments, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 注册并校验单个上游的路由分段。
     *
     * @param routeSegments 已注册分段
     * @param upstreamName 上游名称
     * @param upstream 上游配置
     */
    private void registerRouteSegment(Map<String, String> routeSegments,
                                      String upstreamName,
                                      GatewayProperties.UpstreamProperties upstream) {
        if (!upstream.isEnabled() || (!upstream.isApiEnabled() && !upstream.isActuatorEnabled())) {
            return;
        }
        String normalizedSegment = normalizeRouteSegment(upstream.getRouteSegment());
        String previousUpstream = routeSegments.putIfAbsent(normalizedSegment, upstreamName);
        if (previousUpstream != null) {
            throw new IllegalArgumentException(
                    "gateway route segment must be unique among enabled upstreams: " + normalizedSegment);
        }
    }

    /**
     * 校验内部运维前缀配置。
     *
     * @param properties 网关配置
     */
    private void validateInternalPrefix(GatewayProperties properties) {
        for (GatewayProperties.UpstreamProperties upstream : properties.getUpstreams().values()) {
            if (upstream.isEnabled() && upstream.isActuatorEnabled()) {
                requireText(properties.getInternalPrefix(),
                        "gateway internal prefix must not be blank when actuator route is enabled");
                return;
            }
        }
    }

    /**
     * 校验跨域配置。
     *
     * @param properties 网关配置
     */
    private void validateCors(GatewayProperties properties) {
        GatewayProperties.CorsProperties cors = properties.getCors();
        if (!cors.isEnabled()) {
            return;
        }
        if (isBlankCollection(cors.getAllowedOriginPatterns())) {
            throw new IllegalArgumentException("gateway cors allowed origin patterns must not be empty");
        }
        if (isBlankCollection(cors.getAllowedMethods())) {
            throw new IllegalArgumentException("gateway cors allowed methods must not be empty");
        }
        if (isBlankCollection(cors.getAllowedHeaders())) {
            throw new IllegalArgumentException("gateway cors allowed headers must not be empty");
        }
    }

    /**
     * 校验字符串非空。
     *
     * @param value 待校验值
     * @param message 异常消息
     */
    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 校验 URI 非空。
     *
     * @param value 待校验值
     * @param message 异常消息
     */
    private void requireUri(URI value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 判断集合中是否不存在有效文本元素。
     *
     * @param values 待校验集合
     * @return 是否为空
     */
    private boolean isBlankCollection(Iterable<String> values) {
        if (values == null) {
            return true;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 规范化前缀路径。
     *
     * @param prefix 原始前缀
     * @return 规范化后的前缀
     */
    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /**
     * 规范化路由分段。
     *
     * @param routeSegment 原始分段
     * @return 规范化后的分段
     */
    private String normalizeRouteSegment(String routeSegment) {
        requireText(routeSegment, "gateway route segment must not be blank");
        String normalized = routeSegment.startsWith("/") ? routeSegment.substring(1) : routeSegment;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
