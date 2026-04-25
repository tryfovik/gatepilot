package com.dt.gatepilot.apiserver.domain.audit;

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
     * TraceId。
     */
    private String traceId;

    /**
     * 执行结果。
     */
    private String outcome;

    /**
     * 游标。
     */
    private String cursor;

    /**
     * 返回条数。
     */
    private int limit;
}
