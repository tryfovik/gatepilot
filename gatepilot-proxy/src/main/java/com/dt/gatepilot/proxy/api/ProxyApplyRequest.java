package com.dt.gatepilot.proxy.api;

import com.dt.gatepilot.api.resource.publish.PublishedConfig;
import lombok.Data;

/**
 * proxy 本机应用配置请求。
 */
@Data
public class ProxyApplyRequest {

    /**
     * 已发布配置。
     */
    private PublishedConfig publishedConfig;

    /**
     * 是否只校验不切换运行态。
     */
    private boolean dryRun;
}
