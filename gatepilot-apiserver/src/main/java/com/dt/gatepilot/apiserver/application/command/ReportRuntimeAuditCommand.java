package com.dt.gatepilot.apiserver.application.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * agent 上报运行审计命令。
 */
@Data
public class ReportRuntimeAuditCommand {

    /**
     * 命名空间。
     */
    @NotBlank
    private String namespace;

    /**
     * 节点标识。
     */
    @NotBlank
    private String nodeId;

    /**
     * 审计事件列表。
     */
    @Valid
    private List<RuntimeAuditItem> events = new ArrayList<>();

    /**
     * 运行审计事件。
     */
    @Data
    public static class RuntimeAuditItem {

        /**
         * TraceId。
         */
        private String traceId;

        /**
         * 客户端 IP。
         */
        private String clientIp;

        /**
         * HTTP 方法。
         */
        private String method;

        /**
         * 请求路径。
         */
        private String path;

        /**
         * 请求域名。
         */
        private String host;

        /**
         * 路由标识。
         */
        private String routeId;

        /**
         * 项目名称。
         */
        private String projectName;

        /**
         * 上游名称。
         */
        private String upstreamName;

        /**
         * 上游地址。
         */
        private String upstreamUri;

        /**
         * 响应状态码。
         */
        private Integer status;

        /**
         * 请求延迟。
         */
        private Long latencyMillis;

        /**
         * 流量颜色。
         */
        private String trafficColor;

        /**
         * 方法是否允许。
         */
        private Boolean methodAllowed;

        /**
         * 是否需要认证。
         */
        private Boolean authenticationRequired;

        /**
         * 是否 fallback。
         */
        private Boolean fallback;

        /**
         * 执行结果。
         */
        private String outcome;

        /**
         * 结果原因。
         */
        private String reason;

        /**
         * 异常类型。
         */
        private String error;

        /**
         * 发生时间。
         */
        private Instant occurredAt;
    }
}
