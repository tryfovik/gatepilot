package com.dt.gatepilot.apiserver.application.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 发布 dry-run 校验响应。
 */
@Data
public class ReleaseDryRunResult {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * 预估发布版本。
     */
    private String version;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 校验是否通过。
     */
    private boolean passed;

    /**
     * 路由数量。
     */
    private int routeCount;

    /**
     * 上游数量。
     */
    private int upstreamCount;

    /**
     * 策略数量。
     */
    private int policyCount;

    /**
     * dry-run 时间。
     */
    private Instant checkedAt;

    /**
     * 校验消息。
     */
    private List<DryRunMessage> messages = new ArrayList<>();

    /**
     * dry-run 校验消息。
     */
    @Data
    public static class DryRunMessage {

        /**
         * 级别：INFO / WARN / ERROR。
         */
        private String level;

        /**
         * 机器可读原因码。
         */
        private String reason;

        /**
         * 说明。
         */
        private String message;
    }
}
