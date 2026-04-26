package com.dt.gatepilot.apiserver.application.dto;

import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.enums.ReleaseStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 项目接入模板渲染请求
 */
@Data
public class ProjectTemplateRenderRequest {

    /**
     * 命名空间
     */
    @NotBlank
    private String namespace;

    /**
     * 项目名称
     */
    @NotBlank
    private String projectName;

    /**
     * 展示名称
     */
    private String displayName;

    /**
     * 负责团队
     */
    private String ownerTeam;

    /**
     * 环境
     */
    private String environment;

    /**
     * 流量等级
     */
    private String trafficTier;

    /**
     * 配置分片
     */
    private String configShard;

    /**
     * 隔离组
     */
    private String isolationGroup;

    /**
     * 路由配置
     */
    @Valid
    private RouteValues route = new RouteValues();

    /**
     * 稳定上游配置
     */
    @Valid
    private UpstreamValues upstream = new UpstreamValues();

    /**
     * 候选上游配置
     */
    @Valid
    private CandidateUpstreamValues candidate = new CandidateUpstreamValues();

    /**
     * 治理配置
     */
    @Valid
    private GovernanceValues governance = new GovernanceValues();

    /**
     * 发布配置
     */
    @Valid
    private ReleaseValues release = new ReleaseValues();

    /**
     * 认证配置
     */
    @Valid
    private AuthValues auth = new AuthValues();

    /**
     * 路由 values
     */
    @Data
    public static class RouteValues {

        /**
         * 入口域名
         */
        private String host;

        /**
         * 路径前缀
         */
        private String path;

        /**
         * 是否去除前缀
         */
        private Boolean stripPrefix = true;

        /**
         * HTTP 方法
         */
        private List<HttpMethod> methods = new ArrayList<>(List.of(HttpMethod.ANY));
    }

    /**
     * 上游 values
     */
    @Data
    public static class UpstreamValues {

        /**
         * 上游主机
         */
        private String host;

        /**
         * 上游端口
         */
        @Min(1)
        @Max(65535)
        private Integer port;

        /**
         * 上游协议
         */
        private Protocol protocol = Protocol.HTTP;

        /**
         * 负载均衡策略
         */
        private LoadBalanceStrategy loadBalance = LoadBalanceStrategy.ROUND_ROBIN;

        /**
         * 是否启用健康检查
         */
        private Boolean healthCheckEnabled = true;

        /**
         * 健康检查路径
         */
        private String healthPath;
    }

    /**
     * 候选上游 values
     */
    @Data
    public static class CandidateUpstreamValues {

        /**
         * 是否启用候选上游
         */
        private Boolean enabled = false;

        /**
         * 候选上游主机
         */
        private String host;

        /**
         * 候选上游端口
         */
        @Min(1)
        @Max(65535)
        private Integer port;
    }

    /**
     * 治理 values
     */
    @Data
    public static class GovernanceValues {

        /**
         * 是否启用限流
         */
        private Boolean rateLimitEnabled = false;

        /**
         * 每秒请求数
         */
        @Min(1)
        private Integer requestsPerSecond;

        /**
         * 突发容量
         */
        @Min(1)
        private Integer burstCapacity;

        /**
         * 是否启用重试
         */
        private Boolean retryEnabled = false;

        /**
         * 最大重试次数
         */
        @Min(1)
        private Integer maxAttempts;
    }

    /**
     * 发布 values
     */
    @Data
    public static class ReleaseValues {

        /**
         * 是否创建发布策略
         */
        private Boolean enabled = false;

        /**
         * 发布策略
         */
        private ReleaseStrategy strategy = ReleaseStrategy.TRAFFIC_SPLIT;

        /**
         * 候选流量权重
         */
        @Min(0)
        @Max(100)
        private Integer candidateWeight;

        /**
         * 染色请求头
         */
        private String colorHeader;

        /**
         * 候选染色值
         */
        private String candidateColor;
    }

    /**
     * 认证 values
     */
    @Data
    public static class AuthValues {

        /**
         * 认证类型
         */
        private AuthType type = AuthType.NONE;

        /**
         * 是否允许匿名
         */
        private Boolean anonymousAllowed = true;
    }
}
