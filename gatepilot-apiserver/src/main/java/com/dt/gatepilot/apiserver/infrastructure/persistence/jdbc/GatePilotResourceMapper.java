package com.dt.gatepilot.apiserver.infrastructure.persistence.jdbc;

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
     * @param cursor 游标
     * @param limit 返回条数
     * @return 资源记录列表
     */
    List<GatePilotResourceRecord> selectPageByCursor(@Param("kind") String kind,
                                                     @Param("namespace") String namespace,
                                                     @Param("cursor") String cursor,
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
