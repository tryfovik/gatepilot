package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus.audit;

import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditQuery;
import com.dt.gatepilot.apiserver.domain.audit.RuntimeAuditRecord;
import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverProperties;
import com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus.GatePilotResourceSchemaInitializer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
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

/**
 * MyBatis-Plus 运行审计存储测试。
 */
@SpringBootTest(
        classes = MybatisPlusRuntimeAuditStoreTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "getboot.database.enabled=true",
                "gatepilot.apiserver.store.type=database",
                "gatepilot.apiserver.store.database.initialize-schema=true",
                "mybatis-plus.mapper-locations=classpath*:/mapper/**/*.xml"
        }
)
class MybatisPlusRuntimeAuditStoreTest {

    @Autowired
    private MybatisPlusRuntimeAuditStore store;

    @Test
    void shouldPersistAndQueryRuntimeAudits() {
        String namespace = namespace();
        Instant baseTime = Instant.parse("2026-04-27T00:00:00Z");
        store.saveBatch(List.of(
                record(namespace, "node-1", "trace-1", "project-a", "route-a", "SUCCESS", baseTime.plusSeconds(10)),
                record(namespace, "node-1", "trace-2", "project-b", "route-a", "LIMITED", baseTime.plusSeconds(120))
        ));

        RuntimeAuditQuery query = query(namespace, "route-a", 10);
        query.setProjectName("project-a");
        query.setEndedAt(toLocalDateTime(baseTime.plusSeconds(30)));
        CursorPage<RuntimeAuditRecord> page = store.list(query);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getTraceId()).isEqualTo("trace-1");
        assertThat(page.getItems().get(0).getProjectName()).isEqualTo("project-a");
        assertThat(page.getItems().get(0).getMethodAllowed()).isTrue();
    }

    private RuntimeAuditRecord record(String namespace,
                                      String nodeId,
                                      String traceId,
                                      String projectName,
                                      String routeId,
                                      String outcome,
                                      Instant occurredAt) {
        RuntimeAuditRecord record = new RuntimeAuditRecord();
        // 数据库测试覆盖布尔值、时间和常用过滤字段
        record.setNamespace(namespace);
        record.setNodeId(nodeId);
        record.setTraceId(traceId);
        record.setProjectName(projectName);
        record.setRouteId(routeId);
        record.setOutcome(outcome);
        record.setMethodAllowed(true);
        record.setAuthenticationRequired(false);
        record.setFallback(false);
        record.setOccurredAt(occurredAt);
        return record;
    }

    private RuntimeAuditQuery query(String namespace, String routeId, int limit) {
        RuntimeAuditQuery query = new RuntimeAuditQuery();
        // Console 审计页默认按命名空间和路由过滤
        query.setNamespace(namespace);
        query.setRouteId(routeId);
        query.setLimit(limit);
        return query;
    }

    private String namespace() {
        return "default-" + UUID.randomUUID().toString().replace("-", "");
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /**
     * 测试应用配置。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableConfigurationProperties(GatePilotApiserverProperties.class)
    @MapperScan(basePackageClasses = RuntimeAuditMapper.class)
    @Import({
            MybatisPlusRuntimeAuditStore.class,
            GatePilotResourceSchemaInitializer.class
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
            dataSource.setUrl("jdbc:h2:mem:gatepilot_runtime_audit_"
                    + UUID.randomUUID().toString().replace("-", "_")
                    + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
            dataSource.setUser("sa");
            dataSource.setPassword("");
            return dataSource;
        }
    }
}
