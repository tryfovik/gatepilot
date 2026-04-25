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
