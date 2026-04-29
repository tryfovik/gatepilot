/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.domain.resource.meta;

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
