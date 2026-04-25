package com.dt.platform.gateway;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.management.GatewayConfigSnapshotRepository;
import com.dt.platform.gateway.infrastructure.management.GatewayManagementService;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 网关管理面配置治理服务测试。
 */
class GatewayManagementServiceTest {

    @Test
    void shouldExportCurrentConfigSummary() {
        GatewayProperties properties = createGatewayProperties();
        GatewayManagementService managementService = createManagementService(properties);

        GatewayManagementService.GatewayConfigExportView exportView = managementService.exportConfig();

        assertThat(exportView.exportedAt()).isNotNull();
        assertThat(exportView.properties()).isSameAs(properties);
        assertThat(exportView.summary().projectCount()).isEqualTo(1);
        assertThat(exportView.summary().routeCount()).isEqualTo(1);
        assertThat(exportView.summary().apiRouteCount()).isEqualTo(1);
        assertThat(exportView.summary().internalRouteCount()).isEqualTo(1);
    }

    @Test
    void shouldInitializeActiveSnapshotAndReleaseRecord() {
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        List<GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord> versions =
                managementService.listConfigVersions("ACTIVE", 10);
        List<GatewayConfigSnapshotRepository.GatewayConfigReleaseRecord> records =
                managementService.listReleaseRecords("BOOTSTRAP_ACTIVE_CONFIG", "ACTIVE", 10);

        assertThat(versions).hasSize(1);
        assertThat(versions.get(0).active()).isTrue();
        assertThat(versions.get(0).summary().routeCount()).isEqualTo(1);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).versionId()).isEqualTo(versions.get(0).versionId());
    }

    @Test
    void shouldValidateCandidateConfigByDryRun() {
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayManagementService.GatewayConfigValidationView validation =
                managementService.validate(createGatewayProperties());

        assertThat(validation.valid()).isTrue();
        assertThat(validation.errors()).isEmpty();
        assertThat(validation.summary().routeCount()).isEqualTo(1);
    }

    @Test
    void shouldReportValidationErrorsForInvalidCandidateConfig() {
        GatewayProperties candidate = createGatewayProperties();
        candidate.setInternalPrefix("/api");
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayManagementService.GatewayConfigValidationView validation = managementService.validate(candidate);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.errors()).contains("gateway api prefix and internal prefix must not be identical");
        assertThat(validation.summary()).isNull();
    }

    @Test
    void shouldDiffRouteChanges() {
        GatewayProperties candidate = createGatewayProperties();
        candidate.getProjects().get("game").getRoutes().get("admin")
                .setServiceUri(URI.create("http://127.0.0.1:28080"));
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayManagementService.GatewayConfigDiffView diff = managementService.diff(candidate);

        assertThat(diff.valid()).isTrue();
        assertThat(diff.currentSummary().routeCount()).isEqualTo(1);
        assertThat(diff.candidateSummary().routeCount()).isEqualTo(1);
        assertThat(diff.changes()).anySatisfy(change -> {
            assertThat(change.category()).isEqualTo("route");
            assertThat(change.projectKey()).isEqualTo("game");
            assertThat(change.routeKey()).isEqualTo("admin");
            assertThat(change.field()).isEqualTo("serviceUri");
            assertThat(change.before()).isEqualTo(URI.create("http://127.0.0.1:18080"));
            assertThat(change.after()).isEqualTo(URI.create("http://127.0.0.1:28080"));
        });
    }

    @Test
    void shouldDiffAddedRoutes() {
        GatewayProperties candidate = createGatewayProperties();
        GatewayProperties.RouteProperties openRoute = new GatewayProperties.RouteProperties();
        openRoute.setPathSegment("open");
        openRoute.setServiceUri(URI.create("http://127.0.0.1:18081"));
        openRoute.setActuatorUri(URI.create("http://127.0.0.1:18081"));
        candidate.getProjects().get("game").getRoutes().put("open", openRoute);
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayManagementService.GatewayConfigDiffView diff = managementService.diff(candidate);

        assertThat(diff.valid()).isTrue();
        assertThat(diff.candidateSummary().routeCount()).isEqualTo(2);
        assertThat(diff.changes()).anySatisfy(change -> {
            assertThat(change.category()).isEqualTo("route");
            assertThat(change.projectKey()).isEqualTo("game");
            assertThat(change.routeKey()).isEqualTo("open");
            assertThat(change.field()).isEqualTo("route");
            assertThat(change.before()).isNull();
            assertThat(change.after()).isInstanceOf(GatewayManagementService.RouteSnapshot.class);
        });
    }

    @Test
    void shouldCreateValidCandidateSnapshotWithDiffAndReleaseRecord() {
        GatewayProperties candidate = createGatewayProperties();
        candidate.getProjects().get("game").getRoutes().get("admin")
                .setServiceUri(URI.create("http://127.0.0.1:28080"));
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord snapshot =
                managementService.createCandidateSnapshot(new GatewayManagementService.GatewayConfigSnapshotRequest(
                        "alice",
                        "switch admin upstream",
                        candidate
                ));

        assertThat(snapshot.status()).isEqualTo("VALID");
        assertThat(snapshot.active()).isFalse();
        assertThat(snapshot.operator()).isEqualTo("alice");
        assertThat(snapshot.errors()).isEmpty();
        assertThat(snapshot.changes()).anySatisfy(change -> assertThat(change.field()).isEqualTo("serviceUri"));
        assertThat(managementService.getConfigVersion(snapshot.versionId())).isEqualTo(snapshot);
        assertThat(managementService.listReleaseRecords("CREATE_CANDIDATE_SNAPSHOT", "VALID", 10))
                .anySatisfy(record -> assertThat(record.versionId()).isEqualTo(snapshot.versionId()));
    }

    @Test
    void shouldCreateInvalidCandidateSnapshotWithValidationErrors() {
        GatewayProperties candidate = createGatewayProperties();
        candidate.setInternalPrefix("/api");
        GatewayManagementService managementService = createManagementService(createGatewayProperties());

        GatewayConfigSnapshotRepository.GatewayConfigSnapshotRecord snapshot =
                managementService.createCandidateSnapshot(new GatewayManagementService.GatewayConfigSnapshotRequest(
                        "alice",
                        "bad prefix",
                        candidate
                ));

        assertThat(snapshot.status()).isEqualTo("INVALID");
        assertThat(snapshot.summary()).isNull();
        assertThat(snapshot.errors()).contains("gateway api prefix and internal prefix must not be identical");
        assertThat(snapshot.changes()).isEmpty();
        assertThat(managementService.listConfigVersions("INVALID", 10)).contains(snapshot);
        assertThat(managementService.listReleaseRecords("CREATE_CANDIDATE_SNAPSHOT", "INVALID", 10))
                .anySatisfy(record -> assertThat(record.versionId()).isEqualTo(snapshot.versionId()));
    }

    private GatewayManagementService createManagementService(GatewayProperties properties) {
        return new GatewayManagementService(properties, new GatewayRouteDefinitionLocator(properties));
    }

    private GatewayProperties createGatewayProperties() {
        GatewayProperties properties = new GatewayProperties();
        GatewayProperties.ProjectProperties project = new GatewayProperties.ProjectProperties();
        project.setPathSegment("game");

        GatewayProperties.RouteProperties route = new GatewayProperties.RouteProperties();
        route.setPathSegment("admin");
        route.setServiceUri(URI.create("http://127.0.0.1:18080"));
        route.setServicePathPrefix("/admin");
        route.setActuatorUri(URI.create("http://127.0.0.1:18080"));
        route.setApiMethods(List.of("GET", "POST"));
        route.setInternalMethods(List.of("GET"));
        route.getAuth().setRequired(true);
        route.getAuth().setPublicPaths(List.of("/system/ping"));

        project.getRoutes().put("admin", route);
        properties.getProjects().put("game", project);
        return properties;
    }
}
