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
                    "platform.gateway.upstreams.game-admin.route-segment=admin",
                    "platform.gateway.upstreams.game-admin.service-uri=http://127.0.0.1:18080",
                    "platform.gateway.upstreams.game-admin.actuator-uri=http://127.0.0.1:18080",
                    "platform.gateway.upstreams.game-open.route-segment=open",
                    "platform.gateway.upstreams.game-open.service-uri=http://127.0.0.1:18081",
                    "platform.gateway.upstreams.game-open.actuator-uri=http://127.0.0.1:18081"
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
    void shouldFailWhenEnabledUpstreamsReuseRouteSegment() {
        contextRunner.withPropertyValues("platform.gateway.upstreams.game-open.route-segment=admin")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway route segment must be unique among enabled upstreams");
                });
    }

    @Test
    void shouldFailWhenInternalPrefixMissingWhileActuatorEnabled() {
        contextRunner.withPropertyValues("platform.gateway.internal-prefix=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway internal prefix must not be blank when actuator route is enabled");
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
    void shouldAllowDuplicateSegmentWhenUpstreamDisabled() {
        contextRunner.withPropertyValues(
                        "platform.gateway.upstreams.game-open.enabled=false",
                        "platform.gateway.upstreams.game-open.route-segment=admin"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration
    @EnableConfigurationProperties(GatewayProperties.class)
    static class TestGatewayValidationConfiguration extends GatewayValidationConfiguration {
    }
}
