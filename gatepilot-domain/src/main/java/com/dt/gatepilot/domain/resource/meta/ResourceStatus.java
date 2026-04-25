package com.dt.gatepilot.domain.resource.meta;

import com.dt.gatepilot.domain.enums.ResourcePhase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * GatePilot 资源通用实际状态，对应声明式资源中的 status。
 */
@Data
public class ResourceStatus {

    /**
     * 控制面已经处理到的 generation。
     */
    private Long observedGeneration;

    /**
     * 当前资源生命周期状态。
     */
    private ResourcePhase phase;

    /**
     * 状态条件列表，用于页面诊断和自动化判断。
     */
    private List<ResourceCondition> conditions = new ArrayList<>();

    /**
     * 最近一次状态变化时间。
     */
    private Instant lastTransitionTime;

    /**
     * 最近一次错误码。
     */
    private String reason;

    /**
     * 最近一次错误或告警说明。
     */
    private String message;
}
