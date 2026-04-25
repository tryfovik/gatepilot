package com.dt.gatepilot.apiserver.support.store.jdbc;

import com.dt.gatepilot.api.enums.ResourceKind;
import com.dt.gatepilot.api.resource.project.GatewayProject;
import com.dt.gatepilot.apiserver.api.properties.GatePilotApiserverProperties;
import com.dt.gatepilot.apiserver.api.response.CursorPageResponse;
import com.dt.gatepilot.apiserver.support.resource.ResourceMetadataSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC 资源存储测试。
 */
class JdbcGatePilotResourceStoreTest {

    private final JdbcGatePilotResourceStore store = new JdbcGatePilotResourceStore(
            dataSource(),
            new ResourceMetadataSupport(),
            new ObjectMapper().findAndRegisterModules(),
            properties()
    );

    @Test
    void shouldPersistAndPageResourcesInDatabase() {
        store.initializeSchema();
        GatewayProject first = project("Game Platform");
        GatewayProject second = project("Order Platform");

        store.save(ResourceKind.GATEWAY_PROJECT, "default", "game", first, GatewayProject.class);
        store.save(ResourceKind.GATEWAY_PROJECT, "default", "order", second, GatewayProject.class);
        first.getSpec().setDisplayName("Game Platform Updated");
        store.save(ResourceKind.GATEWAY_PROJECT, "default", "game", first, GatewayProject.class);
        CursorPageResponse<GatewayProject> page =
                store.list(ResourceKind.GATEWAY_PROJECT, "default", null, 1, GatewayProject.class);
        GatewayProject found = store.find(ResourceKind.GATEWAY_PROJECT, "default", "game", GatewayProject.class)
                .orElseThrow();

        assertThat(found.getSpec().getDisplayName()).isEqualTo("Game Platform Updated");
        assertThat(found.getMetadata().getGeneration()).isEqualTo(2L);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getNextCursor()).isEqualTo("default/order");
    }

    private GatewayProject project(String displayName) {
        GatewayProject project = new GatewayProject();
        project.getSpec().setDisplayName(displayName);
        return project;
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setUrl("jdbc:h2:mem:gatepilot_resource_store;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private GatePilotApiserverProperties properties() {
        GatePilotApiserverProperties properties = new GatePilotApiserverProperties();
        properties.getStore().setType("jdbc");
        properties.getStore().getJdbc().setInitializeSchema(true);
        properties.getStore().getJdbc().setTableName("gatepilot_resource");
        return properties;
    }
}
