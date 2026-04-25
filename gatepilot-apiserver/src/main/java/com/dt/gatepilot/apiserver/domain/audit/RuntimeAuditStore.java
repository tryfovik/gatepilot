package com.dt.gatepilot.apiserver.domain.audit;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import java.util.List;

/**
 * 运行审计存储端口。
 */
public interface RuntimeAuditStore {

    /**
     * 批量保存运行审计。
     *
     * @param records 审计记录列表
     */
    void saveBatch(List<RuntimeAuditRecord> records);

    /**
     * 游标分页查询运行审计。
     *
     * @param query 查询条件
     * @return 审计分页结果
     */
    CursorPage<RuntimeAuditRecord> list(RuntimeAuditQuery query);
}
