package com.dt.gatepilot.apiserver.domain.audit;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 运行审计查询条件。
 */
@Data
public class RuntimeAuditQuery {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * 路由标识。
     */
    private String routeId;

    /**
     * 项目名称。
     */
    private String projectName;

    /**
     * TraceId。
     */
    private String traceId;

    /**
     * 执行结果。
     */
    private String outcome;

    /**
     * 开始时间。
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间。
     */
    private LocalDateTime endedAt;

    /**
     * 游标。
     */
    private String cursor;

    /**
     * 返回条数。
     */
    private int limit;
}
