package com.dt.gatepilot.domain.resource.policy;

import com.dt.gatepilot.domain.enums.ReleaseStrategy;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发布策略，描述蓝绿、灰度、权重分流和自动回滚规则。
 */
@Data
public class ReleasePolicy {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private ReleasePolicySpec spec = new ReleasePolicySpec();

    /**
     * 当前状态。
     */
    private ReleasePolicyStatus status = new ReleasePolicyStatus();

    /**
     * 发布策略期望状态。
     */
    @Data
    public static class ReleasePolicySpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 目标路由。
         */
        private List<ResourceReference> routeRefs = new ArrayList<>();

        /**
         * 发布策略类型。
         */
        private ReleaseStrategy strategy;

        /**
         * 稳定版本上游。
         */
        private ResourceReference stableUpstreamRef;

        /**
         * 候选版本上游。
         */
        private ResourceReference candidateUpstreamRef;

        /**
         * 权重分流规则。
         */
        private List<TrafficSplit> trafficSplits = new ArrayList<>();

        /**
         * 分阶段推进计划。
         */
        private List<ReleaseStep> steps = new ArrayList<>();

        /**
         * 自动回滚规则。
         */
        private AutoRollbackRule autoRollback = new AutoRollbackRule();
    }

    /**
     * 权重分流规则。
     */
    @Data
    public static class TrafficSplit {

        /**
         * 分流目标名称，例如 stable、green、canary。
         */
        private String target;

        /**
         * 目标上游。
         */
        private ResourceReference upstreamRef;

        /**
         * 流量权重，取值 0 到 100。
         */
        private Integer weight;

        /**
         * 命中的流量颜色，允许为空。
         */
        private String color;
    }

    /**
     * 发布推进步骤。
     */
    @Data
    public static class ReleaseStep {

        /**
         * 步骤名称。
         */
        private String name;

        /**
         * 候选版本权重。
         */
        private Integer candidateWeight;

        /**
         * 步骤持续观察时间。
         */
        private Duration pauseDuration;

        /**
         * 是否需要人工确认。
         */
        private Boolean manualApprovalRequired;
    }

    /**
     * 自动回滚规则。
     */
    @Data
    public static class AutoRollbackRule {

        /**
         * 是否启用自动回滚。
         */
        private Boolean enabled;

        /**
         * 触发回滚的错误率阈值。
         */
        private Double errorRateThreshold;

        /**
         * 触发回滚的 P95 延迟阈值。
         */
        private Duration p95LatencyThreshold;

        /**
         * 触发回滚前的连续观测窗口数。
         */
        private Integer consecutiveWindows;
    }

    /**
     * 发布策略实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ReleasePolicyStatus extends ResourceStatus {

        /**
         * 当前发布步骤名称。
         */
        private String currentStep;

        /**
         * 当前候选版本权重。
         */
        private Integer currentCandidateWeight;

        /**
         * 最近一次发布版本。
         */
        private String latestReleaseVersion;
    }
}
