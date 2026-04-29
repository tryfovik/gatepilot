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

/**
 * MyBatis-Plus 运行审计常量。
 */
public final class MybatisPlusRuntimeAuditConstants {

    /**
     * 审计表名。
     */
    public static final String TABLE_NAME = "gatepilot_runtime_audit";

    private MybatisPlusRuntimeAuditConstants() {
        // MyBatis-Plus 运行审计常量不允许实例化
    }
}
