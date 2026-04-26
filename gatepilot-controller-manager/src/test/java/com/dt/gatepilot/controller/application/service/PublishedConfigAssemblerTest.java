package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PublishedConfig 组装器测试。
 */
class PublishedConfigAssemblerTest {

    @Test
    void shouldRejectUnsupportedLoadBalanceStrategy() {
        GatewayDesiredState desiredState = desiredState(LoadBalanceStrategy.LEAST_CONNECTIONS);

        assertThatThrownBy(() -> new PublishedConfigAssembler().assemble(request(), desiredState))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(PublishedConfigAssemblerConstants.ERROR_UNSUPPORTED_LOAD_BALANCE_PREFIX);
    }

    private ReconcileRequest request() {
        ReconcileRequest request = new ReconcileRequest();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setVersion("v1");
        request.setSequence(1L);
        return request;
    }

    private GatewayDesiredState desiredState(LoadBalanceStrategy strategy) {
        GatewayDesiredState desiredState = new GatewayDesiredState();
        desiredState.setProject(new GatewayProject());
        desiredState.getUpstreams().add(upstream(strategy));
        return desiredState;
    }

    private Upstream upstream(LoadBalanceStrategy strategy) {
        Upstream upstream = new Upstream();
        upstream.getMetadata().setNamespace("default");
        upstream.getMetadata().setName("hash-upstream");
        upstream.getSpec().setLoadBalance(strategy);
        return upstream;
    }
}
