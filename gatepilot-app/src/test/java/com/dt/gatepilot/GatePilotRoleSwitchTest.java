package com.dt.gatepilot;

import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.apiserver.application.service.GatePilotResourceService;
import com.dt.gatepilot.apiserver.interfaces.rest.GatePilotResourceController;
import com.dt.gatepilot.controller.application.service.ReleaseReconcileController;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.interfaces.control.ProxyRuntimeControlController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot 角色开关集成测试
 */
@SpringBootTest(
        classes = GatePilotApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "gatepilot.mode=cluster",
                "gatepilot.apiserver.enabled=false",
                "gatepilot.controller-manager.enabled=false",
                "gatepilot.agent.enabled=false",
                "gatepilot.proxy.enabled=false"
        }
)
class GatePilotRoleSwitchTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldDisableRoleBeansWhenAllRuntimeRolesAreClosed() {
        assertThat(context.getBeansOfType(GatePilotResourceController.class)).isEmpty();
        assertThat(context.getBeansOfType(GatePilotResourceService.class)).isEmpty();
        assertThat(context.getBeansOfType(ReleaseReconcileController.class)).isEmpty();
        assertThat(context.getBeansOfType(AgentRuntimeCoordinator.class)).isEmpty();
        assertThat(context.getBeansOfType(ProxyConfigApplier.class)).isEmpty();
        assertThat(context.getBeansOfType(ProxyRuntimeControlController.class)).isEmpty();
    }
}
