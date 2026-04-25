package com.dt.platform.gateway.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理面后端应用启动测试。
 */
@SpringBootTest(classes = GatewayAdminApplication.class)
class GatewayAdminApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void gatewayDataPlaneAutoConfigurationIsNotOnAdminClasspath() {
        assertThat(ClassUtils.isPresent(
                "org.springframework.cloud.gateway.config.GatewayAutoConfiguration",
                getClass().getClassLoader()
        )).isFalse();
    }
}
