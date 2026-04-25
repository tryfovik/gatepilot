package com.dt.gatepilot.domain.resource.event;

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关事件资源，用于记录发布、应用、运行和排障事件。
 */
@Data
public class GatewayEvent {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 事件内容。
     */
    private GatewayEventSpec spec = new GatewayEventSpec();

    /**
     * 事件状态。
     */
    private GatewayEventStatus status = new GatewayEventStatus();

    /**
     * 网关事件内容。
     */
    @Data
    public static class GatewayEventSpec {

        /**
         * 关联资源。
         */
        private ResourceReference involvedObject;

        /**
         * 事件级别。
         */
        private EventSeverity severity;

        /**
         * 事件来源，例如 apiserver、controller-manager、agent、proxy。
         */
        private String source;

        /**
         * 机器可读原因码。
         */
        private String reason;

        /**
         * 事件说明。
         */
        private String message;

        /**
         * 首次观测时间。
         */
        private Instant firstObservedAt;

        /**
         * 最近观测时间。
         */
        private Instant lastObservedAt;

        /**
         * 出现次数。
         */
        private Integer count;

        /**
         * 关联 TraceId。
         */
        private String traceId;

        /**
         * 事件扩展信息。
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 网关事件状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GatewayEventStatus extends ResourceStatus {

        /**
         * 是否已归档。
         */
        private Boolean archived;

        /**
         * 确认人。
         */
        private String acknowledgedBy;

        /**
         * 确认时间。
         */
        private Instant acknowledgedAt;
    }
}
