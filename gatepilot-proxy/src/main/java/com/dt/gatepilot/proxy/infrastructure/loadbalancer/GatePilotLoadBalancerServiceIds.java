package com.dt.gatepilot.proxy.infrastructure.loadbalancer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * GatePilot 上游服务标识工具
 */
public final class GatePilotLoadBalancerServiceIds {

    private GatePilotLoadBalancerServiceIds() {
        // 上游服务标识工具不允许实例化
    }

    /**
     * 根据上游名称生成 LoadBalancer serviceId
     *
     * @param upstreamName 上游名称
     * @return LoadBalancer serviceId
     */
    public static String fromUpstreamName(String upstreamName) {
        return GatePilotLoadBalancerConstants.SERVICE_ID_PREFIX
                + digest(Objects.toString(upstreamName, "").trim());
    }

    /**
     * 判断 serviceId 是否属于指定上游
     *
     * @param serviceId LoadBalancer serviceId
     * @param upstreamName 上游名称
     * @return 是否匹配
     */
    public static boolean matches(String serviceId, String upstreamName) {
        return serviceId != null && serviceId.equals(fromUpstreamName(upstreamName));
    }

    /**
     * 计算上游名称摘要
     *
     * @param value 上游名称
     * @return 上游名称摘要
     */
    private static String digest(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    GatePilotLoadBalancerConstants.SERVICE_ID_HASH_ALGORITHM);
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, GatePilotLoadBalancerConstants.SERVICE_ID_HASH_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(GatePilotLoadBalancerConstants.ERROR_HASH_ALGORITHM_MISSING, exception);
        }
    }
}
