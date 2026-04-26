package com.dt.gatepilot.apiserver.application.dto;

import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 项目接入模板预览响应
 */
@Data
public class ProjectTemplatePreviewResponse {

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 配置分片
     */
    private String configShard;

    /**
     * 渲染出的资源
     */
    private List<RenderedResource> resources = new ArrayList<>();

    /**
     * 发布请求预览
     */
    private CreateReleaseCommand releaseRequest;

    /**
     * 资源差异摘要
     */
    private TemplateDiff diff = new TemplateDiff();

    /**
     * 渲染资源
     */
    @Data
    public static class RenderedResource {

        /**
         * 资源类型
         */
        private String resourceType;

        /**
         * 资源 Kind
         */
        private String kind;

        /**
         * 命名空间
         */
        private String namespace;

        /**
         * 资源名称
         */
        private String name;

        /**
         * 差异动作
         */
        private String action;

        /**
         * 资源内容
         */
        private Object resource;
    }

    /**
     * 模板资源差异
     */
    @Data
    public static class TemplateDiff {

        /**
         * 是否存在变更
         */
        private boolean changed;

        /**
         * 新增资源数
         */
        private int createCount;

        /**
         * 更新资源数
         */
        private int updateCount;

        /**
         * 未变化资源数
         */
        private int unchangedCount;
    }
}
