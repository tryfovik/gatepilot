package com.dt.gatepilot.domain.resource.common;

import com.dt.gatepilot.domain.enums.ResourceKind;
import lombok.Data;

/**
 * 资源引用，用于描述资源之间的弱关联。
 */
@Data
public class ResourceReference {

    /**
     * 被引用资源类型。
     */
    private ResourceKind kind;

    /**
     * 被引用资源命名空间。
     */
    private String namespace;

    /**
     * 被引用资源名称。
     */
    private String name;

    /**
     * 被引用资源唯一标识，允许为空。
     */
    private String uid;
}
