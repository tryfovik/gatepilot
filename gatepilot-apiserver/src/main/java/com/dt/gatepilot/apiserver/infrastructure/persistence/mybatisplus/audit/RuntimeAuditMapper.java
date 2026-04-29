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
package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus.audit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 运行审计 MyBatis-Plus Mapper。
 */
@Mapper
public interface RuntimeAuditMapper extends BaseMapper<RuntimeAuditEntity> {

    /**
     * 游标分页查询运行审计。
     *
     * @param query 查询条件
     * @param cursor 游标
     * @param limit 返回条数
     * @return 审计记录列表
     */
    List<RuntimeAuditEntity> selectPageByCursor(@Param("query") RuntimeAuditQuery query,
                                                @Param("cursor") long cursor,
                                                @Param("limit") int limit);

    /**
     * 统计运行审计总数。
     *
     * @param query 查询条件
     * @return 总数
     */
    int countByQuery(@Param("query") RuntimeAuditQuery query);
}
