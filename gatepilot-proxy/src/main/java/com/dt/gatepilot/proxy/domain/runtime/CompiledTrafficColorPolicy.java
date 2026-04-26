package com.dt.gatepilot.proxy.domain.runtime;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译流量染色策略。
 */
@Data
public class CompiledTrafficColorPolicy {

    /**
     * 是否信任请求头染色。
     */
    private boolean trustRequestHeader;

    /**
     * 染色请求头名称。
     */
    private String headerName = TrafficColorConstants.DEFAULT_HEADER_NAME;

    /**
     * 默认流量颜色。
     */
    private String defaultColor = TrafficColorConstants.DEFAULT_COLOR;

    /**
     * 显式染色规则。
     */
    private List<CompiledTrafficColorRule> rules = new ArrayList<>();

    /**
     * 权重染色策略。
     */
    private List<CompiledWeightedTrafficPolicy> weightedPolicies = new ArrayList<>();
}
