package com.dt.gatepilot.proxy.domain.runtime;

import java.util.List;

/**
 * proxy 预编译认证策略。
 *
 * @param requiresAuthentication 是否需要认证
 * @param publicPathPrefixes 公开路径前缀
 */
public record CompiledAuthPolicy(boolean requiresAuthentication, List<String> publicPathPrefixes) {

    /**
     * 创建预编译认证策略。
     *
     * @param requiresAuthentication 是否需要认证
     * @param publicPathPrefixes 公开路径前缀
     */
    public CompiledAuthPolicy {
        publicPathPrefixes = publicPathPrefixes == null ? List.of() : List.copyOf(publicPathPrefixes);
    }

    /**
     * 判断请求路径是否为公开路径。
     *
     * @param normalizedRequestPath 标准化请求路径
     * @return 是否为公开路径
     */
    public boolean publicPath(String normalizedRequestPath) {
        if (normalizedRequestPath == null) {
            return false;
        }
        for (String publicPathPrefix : publicPathPrefixes) {
            if (normalizedRequestPath.equals(publicPathPrefix)
                    || normalizedRequestPath.startsWith(publicPathPrefix + ProxyPathConstants.PATH_SEPARATOR)) {
                return true;
            }
        }
        return false;
    }
}
