package com.dt.gatepilot.domain.resource.project;

import com.dt.gatepilot.domain.resource.common.ResourceMetadata;
import com.dt.gatepilot.domain.resource.common.ResourceStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关项目资源，用于隔离一组路由、上游、策略和发布。
 */
@Data
public class GatewayProject {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private GatewayProjectSpec spec = new GatewayProjectSpec();

    /**
     * 当前状态。
     */
    private GatewayProjectStatus status = new GatewayProjectStatus();

    /**
     * 网关项目期望状态。
     */
    @Data
    public static class GatewayProjectSpec {

        /**
         * 展示名称。
         */
        private String displayName;

        /**
         * 项目说明。
         */
        private String description;

        /**
         * 负责团队。
         */
        private String ownerTeam;

        /**
         * 环境标识，例如 dev、test、prod。
         */
        private String environment;

        /**
         * 流量等级，例如 standard、high、critical，由调度和发布策略使用。
         */
        private String trafficTier;

        /**
         * 配置分片键，用于大规模项目按分片下发 PublishedConfig。
         */
        private String configShard;

        /**
         * 隔离组，用于把高流量项目调度到独立网关副本池。
         */
        private String isolationGroup;

        /**
         * 允许绑定的入口域名。
         */
        private List<String> domains = new ArrayList<>();

        /**
         * 项目级配额，例如路由数、QPS 等。
         */
        private Map<String, String> quotas = new LinkedHashMap<>();
    }

    /**
     * 网关项目实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GatewayProjectStatus extends ResourceStatus {

        /**
         * 当前路由数量。
         */
        private Integer routeCount;

        /**
         * 当前上游数量。
         */
        private Integer upstreamCount;

        /**
         * 当前策略数量。
         */
        private Integer policyCount;

        /**
         * 当前生效的已发布配置版本。
         */
        private String currentPublishedVersion;
    }
}
