package com.dt.gatepilot.apiserver.application.dto;

import lombok.Data;

/**
 * 项目接入模板保存响应
 */
@Data
public class ProjectTemplateApplyResponse {

    /**
     * 保存资源数量
     */
    private int savedResourceCount;

    /**
     * dry-run 结果
     */
    private ProjectTemplateDryRunResponse dryRun;
}
