package com.dt.gatepilot.apiserver.api.response;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * GatePilot 资源游标分页响应。
 *
 * @param <T> 列表元素类型
 */
@Data
public class CursorPageResponse<T> {

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
