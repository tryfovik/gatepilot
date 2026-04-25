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
    void shouldFailWhenAuditRecentCapacityIsNonPositive() {
        contextRunner.withPropertyValues("platform.gateway.audit.recent-capacity=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway audit recent capacity must be positive");
                });
    }

    @Test
    void shouldFailWhenTrafficColorDefaultIsInvalid() {
        contextRunner.withPropertyValues(
                        "platform.gateway.traffic-color.enabled=true",
                        "platform.gateway.traffic-color.default-color=green zone"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway traffic color default color is invalid");
                });
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

    @Test
    void shouldFailWhenApiMethodIsUnsupported() {
        contextRunner.withPropertyValues("platform.gateway.projects.game.routes.admin.api-methods[0]=fetch")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway api method is unsupported");
                });
    }

    @Test
    void shouldFailWhenInternalMethodsConfiguredButActuatorRouteDisabled() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.actuator-enabled=false",
                        "platform.gateway.projects.game.routes.admin.internal-methods[0]=GET"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway internal methods require route to be enabled");
                });
    }

    @Test
    void shouldFailWhenApiMaxRequestSizeConfiguredButApiRouteDisabled() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.api-enabled=false",
                        "platform.gateway.projects.game.routes.admin.api-max-request-size=1MB"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway api max request size requires route to be enabled");
                });
    }

    @Test
    void shouldFailWhenInternalMaxRequestSizeIsNonPositive() {
        contextRunner.withPropertyValues("platform.gateway.projects.game.routes.admin.internal-max-request-size=0B")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway internal max request size must be positive");
                });
    }

    @Test
    void shouldFailWhenRetryConfiguredOnDisabledApiRoute() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.api-enabled=false",
                        "platform.gateway.projects.game.routes.admin.governance.retry.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway retry requires api route to be enabled");
                });
    }

    @Test
    void shouldFailWhenRetryMethodUnsupported() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.governance.retry.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.retry.methods[0]=BREW"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway retry method is unsupported");
                });
    }

    @Test
    void shouldFailWhenRetryTriggersAllEmpty() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.governance.retry.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.retry.series[0]=",
                        "platform.gateway.projects.game.routes.admin.governance.retry.exceptions[0]="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway retry series, statuses and exceptions must not all be empty");
                });
    }

    @Test
    void shouldFailWhenRetryBackoffInvalid() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.governance.retry.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.retry.backoff.first-backoff=50ms",
                        "platform.gateway.projects.game.routes.admin.governance.retry.backoff.max-backoff=10ms"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway retry max backoff must not be less than first backoff");
                });
    }

    @Test
    void shouldFailWhenCircuitBreakerConfiguredOnDisabledApiRoute() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.api-enabled=false",
                        "platform.gateway.projects.game.routes.admin.governance.circuit-breaker.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway circuit breaker requires api route to be enabled");
                });
    }

    @Test
    void shouldFailWhenCircuitBreakerStatusCodeUnsupported() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.governance.circuit-breaker.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.circuit-breaker.status-codes[0]=799"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway circuit breaker status code is unsupported");
                });
    }

    @Test
    void shouldFailWhenCircuitBreakerFallbackUriIsNotForward() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.governance.circuit-breaker.enabled=true",
                        "platform.gateway.projects.game.routes.admin.governance.circuit-breaker.fallback-uri=http://127.0.0.1/fallback"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway circuit breaker fallback uri only supports forward scheme");
                });
    }

    @Test
    void shouldFailWhenReleaseVariantConfiguredWithoutTrafficColor() {
        contextRunner.withPropertyValues(
                        "platform.gateway.projects.game.routes.admin.release.variants.green.service-uri=http://127.0.0.1:28080"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway release variants require traffic color to be enabled");
                });
    }

    @Test
    void shouldFailWhenReleaseVariantWeightIsOutOfRange() {
        contextRunner.withPropertyValues(
                        "platform.gateway.traffic-color.enabled=true",
                        "platform.gateway.projects.game.routes.admin.release.variants.green.weight=101"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway release variant weight must be between 0 and 100");
                });
    }

    @Test
    void shouldFailWhenReleaseVariantTotalWeightExceedsOneHundred() {
        contextRunner.withPropertyValues(
                        "platform.gateway.traffic-color.enabled=true",
                        "platform.gateway.projects.game.routes.admin.release.variants.green.weight=70",
                        "platform.gateway.projects.game.routes.admin.release.variants.blue.weight=40"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
                    assertThat(context.getStartupFailure().getMessage())
                            .contains("gateway release variant total weight must not exceed 100");
                });
    }

    @Configuration
    @EnableConfigurationProperties(GatewayProperties.class)
    static class TestGatewayValidationConfiguration extends GatewayValidationConfiguration {
    }
}
