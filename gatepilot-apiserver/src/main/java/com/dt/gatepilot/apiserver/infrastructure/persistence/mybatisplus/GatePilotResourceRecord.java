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
package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * GatePilot 资源表记录。
 */
@Data
@TableName(MybatisPlusResourceStoreConstants.TABLE_NAME)
public class GatePilotResourceRecord {

    /**
     * 自增主键，供 MyBatis-Plus 乐观锁更新使用。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 资源类型。
     */
    private String kind;

    /**
     * 命名空间。
     */
    private String namespace;

    /**
     * 资源名称。
     */
    private String name;

    /**
     * 资源唯一标识。
     */
    private String uid;

    /**
     * 资源 generation，作为 MyBatis-Plus 乐观锁版本字段。
     */
    @Version
    private Long generation;

    /**
     * 资源 JSON 内容。
     */
    private String resourceJson;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
