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
