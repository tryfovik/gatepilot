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
package com.dt.gatepilot.apiserver.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * GatePilot 资源游标分页响应。
 *
 * @param <T> 列表元素类型
 */
@Data
public class CursorPage<T> {

    /**
     * 当前页数据。
     */
    private List<T> items = new ArrayList<>();

    /**
     * 下一页游标，为空表示没有下一页。
     */
    private String nextCursor;

    /**
     * 当前页大小。
     */
    private int limit;

    /**
     * 当前过滤条件下总量。
     */
    private long total;
}
