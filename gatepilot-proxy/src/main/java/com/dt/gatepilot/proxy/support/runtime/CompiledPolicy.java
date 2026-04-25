package com.dt.gatepilot.proxy.support.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * proxy 预编译策略。
 */
@Data
public class CompiledPolicy {

    /**
     * 策略名称。
     */
    private String name;

    /**
     * 策略类型。
     */
    private String type;

    /**
     * 策略配置。
     */
    private Map<String, Object> config = new LinkedHashMap<>();
}
