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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * GatePilot 资源 MyBatis-Plus Mapper。
 */
@Mapper
public interface GatePilotResourceMapper extends BaseMapper<GatePilotResourceRecord> {

    /**
     * 按资源主键查询记录。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @return 资源记录
     */
    GatePilotResourceRecord selectByResourceKey(@Param("kind") String kind,
                                                @Param("namespace") String namespace,
                                                @Param("name") String name);

    /**
     * 按游标分页查询资源。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param cursorNamespace 游标命名空间
     * @param cursorName 游标资源名称
     * @param limit 返回条数
     * @return 资源记录列表
     */
    List<GatePilotResourceRecord> selectPageByCursor(@Param("kind") String kind,
                                                     @Param("namespace") String namespace,
                                                     @Param("cursorNamespace") String cursorNamespace,
                                                     @Param("cursorName") String cursorName,
                                                     @Param("limit") int limit);

    /**
     * 统计资源数量。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @return 资源数量
     */
    int countByKindAndNamespace(@Param("kind") String kind, @Param("namespace") String namespace);

    /**
     * 按资源主键删除记录。
     *
     * @param kind 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @return 删除行数
     */
    int deleteByResourceKey(@Param("kind") String kind,
                            @Param("namespace") String namespace,
                            @Param("name") String name);
}
