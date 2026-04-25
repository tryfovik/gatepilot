package com.dt.gatepilot.apiserver.domain.resource;

import com.dt.gatepilot.domain.enums.ResourceKind;
import lombok.Data;

/**
 * 控制面资源类型注册信息。
 */
@Data
public class GatePilotResourceType {

    /**
     * URL 中使用的资源类型。
     */
    private final String path;

    /**
     * 资源类型枚举。
     */
    private final ResourceKind kind;

    /**
     * Java 资源类型。
     */
    private final Class<?> javaType;
}
