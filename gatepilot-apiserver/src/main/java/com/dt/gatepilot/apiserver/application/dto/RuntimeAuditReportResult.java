package com.dt.gatepilot.apiserver.application.dto;

import lombok.Data;

/**
 * 运行审计上报结果。
 */
@Data
public class RuntimeAuditReportResult {

    /**
     * 接收条数。
     */
    private int acceptedCount;
}
