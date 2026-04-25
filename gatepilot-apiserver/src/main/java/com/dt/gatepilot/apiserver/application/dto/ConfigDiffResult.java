package com.dt.gatepilot.apiserver.application.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 配置版本 diff 响应。
 */
@Data
public class ConfigDiffResult {

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 基线版本。
     */
    private String baseVersion;

    /**
     * 目标版本。
     */
    private String targetVersion;

    /**
     * 配置分片键。
     */
    private String configShard;

    /**
     * 是否存在变更。
     */
    private boolean changed;

    /**
     * 新增路由数。
     */
    private int addedRoutes;

    /**
     * 删除路由数。
     */
    private int removedRoutes;

    /**
     * 修改路由数。
     */
    private int changedRoutes;

    /**
     * 新增上游数。
     */
    private int addedUpstreams;

    /**
     * 删除上游数。
     */
    private int removedUpstreams;

    /**
     * 修改上游数。
     */
    private int changedUpstreams;

    /**
     * 新增策略数。
     */
    private int addedPolicies;

    /**
     * 删除策略数。
     */
    private int removedPolicies;

    /**
     * 修改策略数。
     */
    private int changedPolicies;

    /**
     * 明细列表。
     */
    private List<ConfigDiffItem> items = new ArrayList<>();

    /**
     * diff 明细。
     */
    @Data
    public static class ConfigDiffItem {

        /**
         * 资源类型：route / upstream / policy。
         */
        private String resourceType;

        /**
         * 资源标识。
         */
        private String name;

        /**
         * 变更类型：ADDED / REMOVED / CHANGED。
         */
        private String changeType;

        /**
         * 基线版本哈希。
         */
        private String baseHash;

        /**
         * 目标版本哈希。
         */
        private String targetHash;
    }
}
