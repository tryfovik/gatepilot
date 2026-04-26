package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.Protocol;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译上游。
 */
@Data
public class CompiledUpstream {

    /**
     * 上游名称。
     */
    private String name;

    /**
     * 上游协议。
     */
    private Protocol protocol;

    /**
     * 负载均衡策略。
     */
    private String loadBalance;

    /**
     * 上游端点。
     */
    private List<CompiledEndpoint> endpoints = new ArrayList<>();

    /**
     * 主动健康检查配置。
     */
    private CompiledHealthCheck healthCheck = new CompiledHealthCheck();

    /**
     * 预编译上游端点。
     */
    @Data
    public static class CompiledEndpoint {

        /**
         * 主机名或 IP。
         */
        private String host;

        /**
         * 端口。
         */
        private Integer port;

        /**
         * 权重。
         */
        private Integer weight;
    }

    /**
     * 预编译健康检查配置。
     */
    @Data
    public static class CompiledHealthCheck {

        /**
         * 是否启用健康检查。
         */
        private Boolean enabled;

        /**
         * 健康检查路径。
         */
        private String path;

        /**
         * 请求超时时间。
         */
        private Duration timeout;

        /**
         * 连续成功阈值。
         */
        private Integer healthyThreshold;

        /**
         * 连续失败阈值。
         */
        private Integer unhealthyThreshold;
    }
}
