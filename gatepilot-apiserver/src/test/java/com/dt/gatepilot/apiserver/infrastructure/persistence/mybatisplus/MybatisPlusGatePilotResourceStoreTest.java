package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverProperties;
import com.dt.gatepilot.domain.enums.ResourceKind;
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
        GatewayProject found = store.find(ResourceKind.GATEWAY_PROJECT, namespace, "game", GatewayProject.class)
                .orElseThrow();

        assertThat(found.getSpec().getDisplayName()).isEqualTo("Game Platform Updated");
        assertThat(found.getMetadata().getUid()).isEqualTo(firstUid);
        assertThat(found.getMetadata().getGeneration()).isEqualTo(2L);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getNextCursor()).isEqualTo(namespace + "/order");
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
