package com.dt.gatepilot.proxy.support.runtime;

import com.dt.gatepilot.api.enums.Protocol;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * proxy 预编译路由，业务请求热路径只读取这个不可变快照。
 */
@Data
public class CompiledRoute {

    /**
     * 路由标识。
     */
    private String routeId;

    /**
     * 入口协议。
     */
    private List<Protocol> protocols = new ArrayList<>();

    /**
     * 匹配域名。
     */
    private List<String> hosts = new ArrayList<>();

    /**
     * 路径前缀。
     */
    private String pathPrefix;

    /**
     * 目标上游名称。
     */
    private String upstreamName;

    /**
     * 策略名称。
     */
    private List<String> policyNames = new ArrayList<>();
}
