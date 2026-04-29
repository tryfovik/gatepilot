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
package com.dt.gatepilot.controller.domain.port;

import com.dt.gatepilot.controller.domain.model.ReleaseIntent;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;

/**
 * 回滚配置读取端口。
 */
public interface RollbackConfigReader {

    /**
     * 读取回滚目标版本的已发布配置。
     *
     * @param intent 发布意图
     * @return 已发布配置
     */
    PublishedConfig readRollbackConfig(ReleaseIntent intent);
}
