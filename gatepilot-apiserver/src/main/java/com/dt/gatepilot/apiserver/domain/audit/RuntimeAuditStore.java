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
