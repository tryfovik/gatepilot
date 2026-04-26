package com.dt.gatepilot.demo.infrastructure.config;

import com.dt.gatepilot.demo.application.service.DemoEchoConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 示例上游配置
 */
@Data
@Component
@ConfigurationProperties(prefix = DemoUpstreamConstants.PROPERTIES_PREFIX)
public class DemoUpstreamProperties {

    /**
     * 服务名称
     */
    private String serviceName = DemoEchoConstants.DEFAULT_SERVICE_NAME;

    /**
     * 实例版本
     */
    private String version = DemoEchoConstants.DEFAULT_VERSION;

    /**
     * 流量颜色
     */
    private String color = DemoEchoConstants.DEFAULT_COLOR;

    /**
     * 实例标识
     */
    private String instanceId = DemoEchoConstants.DEFAULT_INSTANCE_ID;

    /**
     * 部署区域
     */
    private String zone = DemoEchoConstants.DEFAULT_ZONE;

    /**
     * 示例提示
     */
    private String message = DemoEchoConstants.DEFAULT_MESSAGE;
}
