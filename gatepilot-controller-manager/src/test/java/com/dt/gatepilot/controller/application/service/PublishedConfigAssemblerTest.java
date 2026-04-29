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
package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.controller.application.command.ReconcileRequest;
import com.dt.gatepilot.controller.domain.model.GatewayDesiredState;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.RegistryAuthType;
import com.dt.gatepilot.domain.enums.RegistryCenterType;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.enums.UpstreamDiscoveryType;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.platform.RegistryCenter;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void shouldSnapshotNacosDiscoveryIntoPublishedConfig() {
        GatewayDesiredState desiredState = new GatewayDesiredState();
        desiredState.setProject(new GatewayProject());
        desiredState.getRegistryCenters().add(registryCenter());
        desiredState.getUpstreams().add(nacosUpstream());

        PublishedConfig config = new PublishedConfigAssembler().assemble(request(), desiredState);

        PublishedConfig.PublishedDiscovery discovery = config.getSpec().getUpstreams().get(0).getDiscovery();
        assertThat(discovery.getType()).isEqualTo(UpstreamDiscoveryType.NACOS);
        assertThat(discovery.getRegistryType()).isEqualTo(RegistryCenterType.NACOS);
        assertThat(discovery.getServerAddr()).isEqualTo("nacos.prod:8848");
        assertThat(discovery.getServiceName()).isEqualTo("order-service");
        assertThat(discovery.getGroup()).isEqualTo("DEFAULT_GROUP");
        assertThat(discovery.getUsername()).isEqualTo("gatepilot");
        assertThat(discovery.getPassword()).isEqualTo("secret");
        assertThat(config.getSpec().getUpstreams().get(0).getEndpoints()).isEmpty();
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

    private Upstream nacosUpstream() {
        Upstream upstream = upstream(LoadBalanceStrategy.ROUND_ROBIN);
        upstream.getSpec().getDiscovery().setType(UpstreamDiscoveryType.NACOS);
        upstream.getSpec().getDiscovery().setRegistryRef(ref(ResourceKind.REGISTRY_CENTER, "system", "nacos-prod"));
        upstream.getSpec().getDiscovery().setServiceName("order-service");
        upstream.getSpec().getDiscovery().getMetadataSelector().put("version", "stable");
        return upstream;
    }

    private RegistryCenter registryCenter() {
        RegistryCenter registryCenter = new RegistryCenter();
        registryCenter.getMetadata().setNamespace("system");
        registryCenter.getMetadata().setName("nacos-prod");
        registryCenter.getSpec().setType(RegistryCenterType.NACOS);
        registryCenter.getSpec().setServerAddr("nacos.prod:8848");
        registryCenter.getSpec().setGroup("DEFAULT_GROUP");
        registryCenter.getSpec().setAuthType(RegistryAuthType.USERNAME_PASSWORD);
        registryCenter.getSpec().setUsername("gatepilot");
        registryCenter.getSpec().setPassword("secret");
        return registryCenter;
    }

    private ResourceReference ref(ResourceKind kind, String namespace, String name) {
        ResourceReference reference = new ResourceReference();
        reference.setKind(kind);
        reference.setNamespace(namespace);
        reference.setName(name);
        return reference;
    }
}
