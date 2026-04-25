package com.dt.gatepilot.proxy.domain.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译重试策略。
 */
@Data
public class CompiledRetryPolicy {

    /**
     * 策略名称。
     */
    private String name;

    /**
     * 是否启用。
     */
    private boolean enabled;

    /**
     * 最大尝试次数。
     */
    private int maxAttempts;

    /**
     * 可重试状态码。
     */
    private List<Integer> statuses = new ArrayList<>();

    /**
     * 首次退避时间。
     */
    private Duration firstBackoff;

    /**
     * 最大退避时间。
     */
    private Duration maxBackoff;
}
