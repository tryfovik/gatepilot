package com.dt.gatepilot.domain.resource.platform;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台环境资源，用于项目接入时选择运行环境
 */
@Data
public class PlatformEnvironment {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private PlatformEnvironmentSpec spec = new PlatformEnvironmentSpec();

    /**
     * 当前状态
     */
    private PlatformEnvironmentStatus status = new PlatformEnvironmentStatus();

    /**
     * 环境期望状态
     */
    @Data
    public static class PlatformEnvironmentSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 环境说明
         */
        private String description;

        /**
         * 环境等级
         */
        private String tier;

        /**
         * 默认配置分片
         */
        private String defaultConfigShard;

        /**
         * 默认隔离组
         */
        private String defaultIsolationGroup;

        /**
         * 是否允许新项目接入
         */
        private Boolean acceptingProjects = true;

        /**
         * 环境扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 环境实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PlatformEnvironmentStatus extends ResourceStatus {

        /**
         * 项目数量
         */
        private Integer projectCount;
    }
}
