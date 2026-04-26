package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfigConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.StringUtils;

/**
 * 根据发布策略解析实际转发上游。
 */
public class ReleaseUpstreamResolver {

    /**
     * 解析路由当前应转发的上游名称。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @param trafficColor 流量颜色
     * @return 上游名称
     */
    public String resolve(CompiledProxyRuntime runtime, CompiledRoute route, String trafficColor) {
        if (runtime == null || route == null) {
            return null;
        }
        String normalizedColor = normalize(trafficColor);
        if (normalizedColor == null) {
            return route.getUpstreamName();
        }
        String upstreamName = resolveFromPolicy(releasePolicy(runtime, route), normalizedColor);
        if (StringUtils.hasText(upstreamName)) {
            return upstreamName;
        }
        return route.getUpstreamName();
    }

    /**
     * 预编译发布上游策略。
     *
     * @param runtime 已编译运行态
     * @param route 已命中路由
     * @return 发布上游策略
     */
    public CompiledReleaseUpstreamPolicy compile(CompiledProxyRuntime runtime, CompiledRoute route) {
        CompiledReleaseUpstreamPolicy target = new CompiledReleaseUpstreamPolicy();
        if (runtime == null || route == null) {
            return target;
        }
        for (CompiledPolicy policy : policiesByType(runtime, route, PublishedConfigConstants.POLICY_TYPE_RELEASE)) {
            target.getSplits().addAll(trafficSplits(policy));
        }
        return target;
    }

    private CompiledReleaseUpstreamPolicy releasePolicy(CompiledProxyRuntime runtime, CompiledRoute route) {
        if (route.isPoliciesPrecompiled()) {
            return route.getReleaseUpstreamPolicy() == null
                    ? new CompiledReleaseUpstreamPolicy()
                    : route.getReleaseUpstreamPolicy();
        }
        return route.getReleaseUpstreamPolicy() == null ? compile(runtime, route) : route.getReleaseUpstreamPolicy();
    }

    private String resolveFromPolicy(CompiledReleaseUpstreamPolicy policy, String normalizedColor) {
        for (CompiledReleaseUpstreamSplit split : policy.getSplits()) {
            if (split.matches(normalizedColor)) {
                return split.upstreamName();
            }
        }
        return null;
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

    private List<CompiledReleaseUpstreamSplit> trafficSplits(CompiledPolicy policy) {
        Object splits = policy.getConfig().get(PublishedConfigConstants.KEY_TRAFFIC_SPLITS);
        if (!(splits instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<CompiledReleaseUpstreamSplit> parsedSplits = new ArrayList<>();
        for (Object item : iterable) {
            // 单条分流配置异常时跳过，不影响其他分流
            CompiledReleaseUpstreamSplit split = trafficSplit(item);
            if (split != null) {
                parsedSplits.add(split);
            }
        }
        return parsedSplits;
    }

    private CompiledReleaseUpstreamSplit trafficSplit(Object item) {
        if (item instanceof ReleasePolicy.TrafficSplit split) {
            return trafficSplit(split.getTarget(), split.getColor(), upstreamName(split.getUpstreamRef()));
        }
        if (item instanceof Map<?, ?> map) {
            return trafficSplit(
                    stringValue(map, PublishedConfigConstants.KEY_TARGET),
                    stringValue(map, PublishedConfigConstants.KEY_COLOR),
                    upstreamName(map.get(PublishedConfigConstants.KEY_UPSTREAM_REF))
            );
        }
        return null;
    }

    private CompiledReleaseUpstreamSplit trafficSplit(String target, String color, String upstreamName) {
        String normalizedTarget = normalize(target);
        String normalizedColor = normalize(color);
        if (!StringUtils.hasText(upstreamName) || (normalizedTarget == null && normalizedColor == null)) {
            return null;
        }
        return new CompiledReleaseUpstreamSplit(normalizedTarget, normalizedColor, upstreamName.trim());
    }

    private String upstreamName(ResourceReference reference) {
        return reference == null ? null : reference.getName();
    }

    private String upstreamName(Object value) {
        if (value instanceof ResourceReference reference) {
            return upstreamName(reference);
        }
        if (value instanceof Map<?, ?> map) {
            return stringValue(map, PublishedConfigConstants.KEY_NAME);
        }
        return null;
    }

    private String stringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

}
