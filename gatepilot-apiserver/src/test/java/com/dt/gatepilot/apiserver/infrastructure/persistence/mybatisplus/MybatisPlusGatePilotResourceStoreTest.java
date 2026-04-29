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
package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.repository.ResourceStoreConstants;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverProperties;
import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.event.GatewayEventConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.project.GatewayProject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getboot.exception.api.exception.BusinessException;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MyBatis-Plus 资源存储测试。
 */
@SpringBootTest(
        classes = MybatisPlusGatePilotResourceStoreTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "getboot.database.enabled=true",
                "gatepilot.apiserver.store.type=database",
                "gatepilot.apiserver.store.database.initialize-schema=true",
                "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml"
        }
)
class MybatisPlusGatePilotResourceStoreTest {

    private static final String EMPTY_NAMESPACE = "";

    private static final String RELEASE_EVENT_NAME = "rel-1";

    private static final String CONTROLLER_A = "controller-a";

    private static final String CONTROLLER_B = "controller-b";

    @Autowired
    private MybatisPlusGatePilotResourceStore store;

    /**
     * 验证资源可以持久化、更新和游标分页。
     */
    @Test
    void shouldPersistAndPageResourcesInDatabase() {
        String namespace = namespace();
        GatewayProject first = project("Game Platform");
        GatewayProject second = project("Order Platform");

        store.save(ResourceKind.GATEWAY_PROJECT, namespace, "game", first, GatewayProject.class);
        store.save(ResourceKind.GATEWAY_PROJECT, namespace, "order", second, GatewayProject.class);
        String firstUid = first.getMetadata().getUid();
        first.getSpec().setDisplayName("Game Platform Updated");
        store.save(ResourceKind.GATEWAY_PROJECT, namespace, "game", first, GatewayProject.class);
        CursorPage<GatewayProject> page =
                store.list(ResourceKind.GATEWAY_PROJECT, namespace, null, 1, GatewayProject.class);
        CursorPage<GatewayProject> nextPage =
                store.list(ResourceKind.GATEWAY_PROJECT, namespace, page.getNextCursor(), 1, GatewayProject.class);
        GatewayProject found = store.find(ResourceKind.GATEWAY_PROJECT, namespace, "game", GatewayProject.class)
                .orElseThrow();

        assertThat(found.getSpec().getDisplayName()).isEqualTo("Game Platform Updated");
        assertThat(found.getMetadata().getUid()).isEqualTo(firstUid);
        assertThat(found.getMetadata().getGeneration()).isEqualTo(2L);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getNextCursor()).isEqualTo(namespace + "/game");
        assertThat(nextPage.getItems()).hasSize(1);
        assertThat(nextPage.getItems().get(0).getMetadata().getName()).isEqualTo("order");
        assertThat(nextPage.getNextCursor()).isNull();
    }

    /**
     * 验证全命名空间列表遇到空 namespace 游标时不会重复上一页。
     */
    @Test
    void shouldContinueAfterEmptyNamespaceCursor() {
        String projectPrefix = "cursor-" + UUID.randomUUID().toString().replace("-", "");
        String firstName = projectPrefix + "-a";
        String secondName = projectPrefix + "-b";
        GatewayProject first = project("Global Game Platform");
        GatewayProject second = project("Global Order Platform");

        store.save(ResourceKind.GATEWAY_PROJECT, EMPTY_NAMESPACE, firstName, first, GatewayProject.class);
        store.save(ResourceKind.GATEWAY_PROJECT, EMPTY_NAMESPACE, secondName, second, GatewayProject.class);
        CursorPage<GatewayProject> nextPage =
                store.list(ResourceKind.GATEWAY_PROJECT, EMPTY_NAMESPACE,
                        ResourceStoreConstants.CURSOR_SEPARATOR + firstName, 1, GatewayProject.class);

        assertThat(nextPage.getItems()).hasSize(1);
        assertThat(nextPage.getItems().get(0).getMetadata().getName()).isEqualTo(secondName);
    }

    /**
     * 验证旧 generation 的写入会被乐观锁拒绝。
     */
    @Test
    void shouldRejectStaleGenerationUpdate() {
        String namespace = namespace();
        GatewayProject first = project("Game Platform");
        store.save(ResourceKind.GATEWAY_PROJECT, namespace, "game", first, GatewayProject.class);
        GatewayProject stale = store.find(ResourceKind.GATEWAY_PROJECT, namespace, "game", GatewayProject.class)
                .orElseThrow();
        GatewayProject latest = store.find(ResourceKind.GATEWAY_PROJECT, namespace, "game", GatewayProject.class)
                .orElseThrow();

        latest.getSpec().setDisplayName("Fresh Update");
        store.save(ResourceKind.GATEWAY_PROJECT, namespace, "game", latest, GatewayProject.class);
        stale.getSpec().setDisplayName("Stale Update");

        assertThatThrownBy(() -> store.save(ResourceKind.GATEWAY_PROJECT, namespace, "game",
                stale, GatewayProject.class))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(MybatisPlusResourceStoreConstants.MESSAGE_RESOURCE_WRITE_CONFLICT);
        GatewayProject found = store.find(ResourceKind.GATEWAY_PROJECT, namespace, "game", GatewayProject.class)
                .orElseThrow();
        assertThat(found.getSpec().getDisplayName()).isEqualTo("Fresh Update");
        assertThat(found.getMetadata().getGeneration()).isEqualTo(2L);
    }

    /**
     * 验证发布事件 claim 只能被一个 controller 成功写入。
     */
    @Test
    void shouldRejectConcurrentReleaseEventClaimByOptimisticGeneration() {
        String namespace = namespace();
        GatewayEvent pendingEvent = pendingReleaseEvent();
        store.save(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME, pendingEvent, GatewayEvent.class);
        GatewayEvent controllerAView = store.find(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME,
                GatewayEvent.class).orElseThrow();
        GatewayEvent controllerBView = store.find(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME,
                GatewayEvent.class).orElseThrow();

        markProcessing(controllerAView, CONTROLLER_A);
        store.save(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME, controllerAView, GatewayEvent.class);
        markProcessing(controllerBView, CONTROLLER_B);

        assertThatThrownBy(() -> store.save(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME,
                controllerBView, GatewayEvent.class))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(MybatisPlusResourceStoreConstants.MESSAGE_RESOURCE_WRITE_CONFLICT);
        GatewayEvent claimed = store.find(ResourceKind.GATEWAY_EVENT, namespace, RELEASE_EVENT_NAME,
                GatewayEvent.class).orElseThrow();
        assertThat(claimed.getMetadata().getLabels())
                .containsEntry(ResourceMetadataConstants.LABEL_RECONCILE_CONTROLLER, CONTROLLER_A);
        assertThat(claimed.getMetadata().getGeneration()).isEqualTo(2L);
    }

    /**
     * 创建测试项目资源。
     *
     * @param displayName 展示名称
     * @return 项目资源
     */
    private GatewayProject project(String displayName) {
        GatewayProject project = new GatewayProject();
        // 测试只关心 spec 更新后 generation 是否正确
        project.getSpec().setDisplayName(displayName);
        return project;
    }

    /**
     * 创建待处理发布事件。
     *
     * @return 待处理发布事件
     */
    private GatewayEvent pendingReleaseEvent() {
        GatewayEvent event = new GatewayEvent();
        event.getSpec().setSeverity(EventSeverity.INFO);
        event.getSpec().setSource(GatewayEventConstants.SOURCE_APISERVER);
        event.getSpec().setReason(GatewayEventConstants.REASON_CREATE_RELEASE_COMMANDED);
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_RELEASE_ID, RELEASE_EVENT_NAME);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_EVENT_TYPE,
                GatewayEventConstants.EVENT_TYPE_RELEASE_REQUEST);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_PENDING);
        return event;
    }

    /**
     * 标记事件被 controller 抢占。
     *
     * @param event 发布事件
     * @param controllerId controller 标识
     */
    private void markProcessing(GatewayEvent event, String controllerId) {
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_STATE,
                GatewayEventConstants.RECONCILE_STATE_PROCESSING);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_RECONCILE_CONTROLLER, controllerId);
    }

    /**
     * 创建测试命名空间。
     *
     * @return 测试命名空间
     */
    private String namespace() {
        return "default-" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 测试应用配置。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(GatePilotApiserverProperties.class)
    @MapperScan(basePackageClasses = GatePilotResourceMapper.class)
    @Import({
            MybatisPlusGatePilotResourceStore.class,
            GatePilotResourceSchemaInitializer.class,
            ResourceMetadataSupport.class
    })
    static class TestApplication {

        /**
         * 创建测试数据源。
         *
         * @return 测试数据源
         */
        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            // 每个测试上下文使用独立内存库
            dataSource.setUrl("jdbc:h2:mem:gatepilot_resource_store_"
                    + UUID.randomUUID().toString().replace("-", "_")
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            dataSource.setPassword("");
            return dataSource;
        }

        /**
         * 创建 JSON 转换器。
         *
         * @return JSON 转换器
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
