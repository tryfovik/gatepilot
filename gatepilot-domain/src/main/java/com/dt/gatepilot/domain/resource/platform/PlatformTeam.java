package com.dt.gatepilot.domain.resource.platform;

import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 平台团队资源，用于项目接入时选择负责团队
 */
@Data
public class PlatformTeam {

    /**
     * 资源元信息
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态
     */
    private PlatformTeamSpec spec = new PlatformTeamSpec();

    /**
     * 当前状态
     */
    private PlatformTeamStatus status = new PlatformTeamStatus();

    /**
     * 团队期望状态
     */
    @Data
    public static class PlatformTeamSpec {

        /**
         * 展示名称
         */
        private String displayName;

        /**
         * 团队说明
         */
        private String description;

        /**
         * 团队负责人
         */
        private String owner;

        /**
         * 联系方式
         */
        private String contact;

        /**
         * 是否允许接入新项目
         */
        private Boolean acceptingProjects = true;

        /**
         * 团队扩展属性
         */
        private Map<String, String> attributes = new LinkedHashMap<>();
    }

    /**
     * 团队实际状态
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PlatformTeamStatus extends ResourceStatus {

        /**
         * 项目数量
         */
        private Integer projectCount;
    }
}
