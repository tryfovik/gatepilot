package com.dt.gatepilot.apiserver.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 项目接入模板 dry-run 响应
 */
@Data
public class ProjectTemplateDryRunResponse {

    /**
     * 是否通过
     */
    private boolean passed;

    /**
     * 资源预览
     */
    private ProjectTemplatePreviewResponse preview;

    /**
     * 校验消息
     */
    private List<ReleaseDryRunResult.DryRunMessage> messages = new ArrayList<>();
}
