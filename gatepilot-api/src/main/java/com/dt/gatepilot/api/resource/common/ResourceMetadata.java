package com.dt.gatepilot.api.resource.common;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * GatePilot 资源元信息，对应声明式资源中的 metadata。
 */
@Data
public class ResourceMetadata {

    /**
     * 资源唯一标识，由控制面生成。
     */
    private String uid;

    /**
     * 资源名称，同一 namespace 内唯一。
     */
    private String name;

    /**
     * 资源命名空间，用于项目、环境或租户隔离。
     */
    private String namespace;

    /**
     * 租户标识，后续支持多租户管理时使用。
     */
    private String tenant;

    /**
     * 资源版本号，每次 spec 变更时递增。
     */
    private Long generation;

    /**
     * 资源标签，用于筛选和策略绑定。
     */
    private Map<String, String> labels = new LinkedHashMap<>();

    /**
     * 资源注解，用于承载非查询型扩展信息。
     */
    private Map<String, String> annotations = new LinkedHashMap<>();

    /**
     * 创建时间。
     */
    private Instant createdAt;

    /**
     * 最近更新时间。
     */
    private Instant updatedAt;
}
