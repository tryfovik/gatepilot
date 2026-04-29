/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot;

import com.dt.gatepilot.agent.application.service.AgentRuntimeCoordinator;
import com.dt.gatepilot.agent.domain.port.ProxyApplyClient;
import com.dt.gatepilot.agent.domain.port.ProxyRuntimeStatusReader;
import com.dt.gatepilot.agent.infrastructure.scheduling.AgentLifecycleManager;
import com.dt.gatepilot.embedded.infrastructure.assembly.InProcessProxyApplyClient;
import com.dt.gatepilot.proxy.domain.runtime.ProxyConfigApplier;
import com.dt.gatepilot.proxy.infrastructure.loadbalancer.GatePilotReactorServiceInstanceLoadBalancer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot 单体合包装配测试
 */
@SpringBootTest(
        classes = GatePilotApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.port=0",
                "gatepilot.mode=standalone",
                "gatepilot.agent.lifecycle-enabled=false",
                "gatepilot.agent.local-config.store-type=memory",
                "gatepilot.apiserver.store.type=memory",
                "gatepilot.controller-manager.distributed-lock-required=false",
                "management.health.redis.enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,"
                        + "cn.dev33.satoken.dao.SaTokenDaoRedisJackson"
        }
)
class GatePilotStandaloneAssemblyTest {

    @Autowired
    private ApplicationContext context;

    /**
     * 校验单体模式内的控制面、agent 和 proxy 装配
     */
    @Test
    void shouldWireStandaloneControlPlaneAgentAndProxyInOneJvm() {
        assertThat(context.getBeansOfType(ProxyConfigApplier.class)).hasSize(1);
        assertThat(context.getBeansOfType(ProxyApplyClient.class)).hasSize(1);
        assertThat(context.getBean(ProxyApplyClient.class)).isInstanceOf(InProcessProxyApplyClient.class);
        assertThat(context.getBeansOfType(ProxyRuntimeStatusReader.class)).hasSize(1);
        assertThat(context.getBeansOfType(AgentRuntimeCoordinator.class)).hasSize(1);
        assertThat(context.getBeansOfType(AgentLifecycleManager.class)).hasSize(1);
        assertThat(context.getBeansOfType(GatePilotReactorServiceInstanceLoadBalancer.class)).isEmpty();
    }
}
