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
