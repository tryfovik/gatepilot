package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.Protocol;
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
}
