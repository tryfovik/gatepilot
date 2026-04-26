package com.dt.gatepilot.proxy.domain.runtime;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译发布上游策略。
 */
@Data
public class CompiledReleaseUpstreamPolicy {

    /**
     * 发布上游分流列表。
     */
    private List<CompiledReleaseUpstreamSplit> splits = new ArrayList<>();
}
