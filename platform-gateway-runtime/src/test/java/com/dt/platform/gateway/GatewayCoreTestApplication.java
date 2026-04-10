package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 核心模块测试应用。
 */
@SpringBootApplication(scanBasePackages = "com.dt.platform.gateway")
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayCoreTestApplication {
}
