package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.validation.GatewayValidationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关配置启动校验测试。
 */
class GatewayValidationConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestGatewayValidationConfiguration.class)
            .withPropertyValues(
                    "platform.gateway.api-prefix=/api",
                    "platform.gateway.internal-prefix=/internal",
                    "platform.gateway.context-headers.project-header-name=X-Platform-Project",
                    "platform.gateway.context-headers.route-header-name=X-Platform-Route"
            );

    @Test
    void shouldStartWhenGatewayConfigurationIsValid() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GatewayValidationConfiguration.class);
        });
    }

    @Test
    void shouldFailWhenApiPrefixMatchesInternalPrefix() {
        contextRunner.withPropertyValues("platform.gateway.internal-prefix=/api")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway api prefix and internal prefix must not be identical");
                });
    }

    @Test
    void shouldFailWhenContextHeaderNameMissing() {
        contextRunner.withPropertyValues("platform.gateway.context-headers.project-header-name=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway project header name must not be blank");
                });
    }

    @Test
    void shouldFailWhenContextHeaderNamesConflict() {
        contextRunner.withPropertyValues("platform.gateway.context-headers.route-header-name=X-Platform-Project")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway project header name and route header name must be different");
                });
    }

    @Test
    void shouldFailWhenCorsOriginsMissing() {
        contextRunner.withPropertyValues("platform.gateway.cors.allowed-origin-patterns[0]=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway cors allowed origin patterns must not be empty");
                });
    }

    @Test
    void shouldAllowContextHeadersToBeDisabled() {
        contextRunner.withPropertyValues("platform.gateway.context-headers.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void shouldFailWhenFlowControlCountMissing() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.api-enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.flow-control.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway flow control count must be positive");
                });
    }

    @Test
    void shouldFailWhenHeaderParamFlowFieldNameMissing() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.api-enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.flow-control.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.flow-control.count=100",
                        "platform.gateway.projects.game.routes.admin.governance.flow-control.param.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.flow-control.param.parse-strategy=header"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway flow control param field name must not be blank");
                });
    }

    @Configuration
    @EnableConfigurationProperties(GatewayProperties.class)
    static class TestGatewayValidationConfiguration extends GatewayValidationConfiguration {
    }
}
