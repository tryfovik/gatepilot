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
package com.dt.gatepilot.apiserver.application.service;

import com.dt.gatepilot.apiserver.application.command.CreateReleaseCommand;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateApplyResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDefaultsResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateDryRunResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplatePreviewResponse;
import com.dt.gatepilot.apiserver.application.dto.ProjectTemplateRenderRequest;
import com.dt.gatepilot.apiserver.application.dto.ReleaseDryRunResult;
import com.dt.gatepilot.apiserver.domain.resource.GatePilotResourceRegistry;
import com.dt.gatepilot.apiserver.domain.resource.ResourceMetadataSupport;
import com.dt.gatepilot.apiserver.infrastructure.persistence.memory.InMemoryGatePilotResourceStore;
import com.dt.gatepilot.domain.enums.LoadBalanceStrategy;
import com.dt.gatepilot.domain.enums.ReleaseStrategy;
import com.dt.gatepilot.domain.enums.UpstreamDiscoveryType;
import com.dt.gatepilot.domain.resource.policy.ReleasePolicy;
import com.dt.gatepilot.domain.resource.upstream.Upstream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 项目接入模板服务测试
 */
class ProjectTemplateServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GatePilotResourceService resourceService = new GatePilotResourceService(
            new GatePilotResourceRegistry(),
            new InMemoryGatePilotResourceStore(new ResourceMetadataSupport()),
            objectMapper
    );

    private final GatePilotConfigSnapshotService snapshotService =
            new GatePilotConfigSnapshotService(resourceService, objectMapper);

    private final GatePilotReleaseService releaseService =
            new GatePilotReleaseService(resourceService, snapshotService);

    private final ProjectTemplateService templateService =
            new ProjectTemplateService(resourceService);

    @Test
    void shouldPreviewAndApplyProjectTemplateResources() {
        ProjectTemplateRenderRequest request = request();

        ProjectTemplateDryRunResponse dryRun = templateService.dryRun(request);
        ProjectTemplateApplyResponse apply = templateService.apply(request);
        ProjectTemplatePreviewResponse previewAfterApply = templateService.preview(request);
        ProjectTemplateApplyResponse secondApply = templateService.apply(request);
        ReleaseDryRunResult releaseDryRun = releaseService.dryRun(releaseRequest());

        assertThat(dryRun.isPassed()).isTrue();
        assertThat(apply.getSavedResourceCount()).isEqualTo(6);
        assertThat(secondApply.getSavedResourceCount()).isZero();
        assertThat(previewAfterApply.getDiff().isChanged()).isFalse();
        assertThat(previewAfterApply.getResources())
                .extracting(ProjectTemplatePreviewResponse.RenderedResource::getAction)
                .containsOnly(ProjectTemplateConstants.ACTION_UNCHANGED);
        assertThat(releaseDryRun.isPassed()).isTrue();
        assertThat(releaseDryRun.getRouteCount()).isEqualTo(1);
        assertThat(releaseDryRun.getUpstreamCount()).isEqualTo(2);
        assertThat(releaseDryRun.getPolicyCount()).isEqualTo(2);
    }

    @Test
    void shouldRejectUnsupportedLoadBalanceStrategy() {
        ProjectTemplateRenderRequest request = request();
        request.getUpstream().setLoadBalance(LoadBalanceStrategy.CONSISTENT_HASH);

        ProjectTemplateDryRunResponse dryRun = templateService.dryRun(request);

        assertThat(dryRun.isPassed()).isFalse();
        assertThat(dryRun.getMessages())
                .extracting(ReleaseDryRunResult.DryRunMessage::getReason)
                .contains(ProjectTemplateConstants.REASON_TEMPLATE_INVALID);
    }

    @Test
    void shouldRenderBlueGreenAsFullCandidateSwitch() {
        ProjectTemplateRenderRequest request = request();
        request.getRelease().setStrategy(ReleaseStrategy.BLUE_GREEN);
        request.getRelease().setCandidateWeight(20);

        ProjectTemplatePreviewResponse preview = templateService.preview(request);
        ReleasePolicy releasePolicy = preview.getResources()
                .stream()
                .filter(resource -> resource.getKind().equals("RELEASE_POLICY"))
                .map(ProjectTemplatePreviewResponse.RenderedResource::getResource)
                .map(ReleasePolicy.class::cast)
                .findFirst()
                .orElseThrow();

        assertThat(releasePolicy.getSpec().getTrafficSplits())
                .extracting(ReleasePolicy.TrafficSplit::getWeight)
                .containsExactly(0, ProjectTemplateConstants.MAX_TRAFFIC_WEIGHT);
    }

    @Test
    void shouldReturnCentralizedTemplateDefaults() {
        var defaults = templateService.defaults();

        assertThat(defaults.getValues().getProjectName()).isEqualTo(ProjectTemplateConstants.DEFAULT_PROJECT_NAME);
        assertThat(defaults.getValues().getConfigShard()).isEqualTo(ProjectTemplateConstants.DEFAULT_CONFIG_SHARD);
        assertThat(defaults.getValues().getGovernance().getRateLimitEnabled()).isFalse();
        assertThat(defaults.getValues().getGovernance().getRetryEnabled()).isFalse();
        assertThat(defaults.getValues().getRelease().getEnabled()).isFalse();
        assertThat(defaults.getLoadBalances())
                .filteredOn(item -> item.getValue().equals(LoadBalanceStrategy.CONSISTENT_HASH.name()))
                .extracting(ProjectTemplateDefaultsResponse.OptionItem::isEnabled)
                .containsExactly(false);
    }

    @Test
    void shouldRenderNacosUpstreamFromProjectTemplate() {
        ProjectTemplateRenderRequest request = request();
        request.getUpstream().setDiscoveryType(UpstreamDiscoveryType.NACOS);
        request.getUpstream().setRegistryCenterName("nacos-prod");
        request.getUpstream().setServiceName("game-service");
        request.getUpstream().setDiscoveryGroup("GAME_GROUP");
        request.getUpstream().setHost(null);
        request.getUpstream().setPort(null);
        request.getCandidate().setDiscoveryType(UpstreamDiscoveryType.NACOS);
        request.getCandidate().setRegistryCenterName("nacos-prod");
        request.getCandidate().setServiceName("game-service-green");
        request.getCandidate().setHost(null);
        request.getCandidate().setPort(null);

        ProjectTemplateDryRunResponse dryRun = templateService.dryRun(request);
        ProjectTemplatePreviewResponse preview = dryRun.getPreview();

        assertThat(dryRun.isPassed()).isTrue();
        assertThat(preview.getResources())
                .filteredOn(resource -> resource.getKind().equals("UPSTREAM"))
                .map(ProjectTemplatePreviewResponse.RenderedResource::getResource)
                .map(Upstream.class::cast)
                .allSatisfy(upstream -> {
                    assertThat(upstream.getSpec().getEndpoints()).isEmpty();
                    assertThat(upstream.getSpec().getDiscovery().getType()).isEqualTo(UpstreamDiscoveryType.NACOS);
                    assertThat(upstream.getSpec().getDiscovery().getRegistryRef().getName()).isEqualTo("nacos-prod");
                });
    }

    private ProjectTemplateRenderRequest request() {
        ProjectTemplateRenderRequest request = new ProjectTemplateRenderRequest();
        request.setNamespace("default");
        request.setProjectName("game");
        request.setDisplayName("游戏服务");
        request.getRoute().setHost("game.example.com");
        request.getRoute().setPath("/api/game");
        request.getUpstream().setHost("10.0.0.1");
        request.getUpstream().setPort(8080);
        request.getCandidate().setEnabled(true);
        request.getCandidate().setHost("10.0.0.2");
        request.getCandidate().setPort(8080);
        request.getRelease().setEnabled(true);
        request.getRelease().setCandidateWeight(20);
        return request;
    }

    private CreateReleaseCommand releaseRequest() {
        CreateReleaseCommand command = new CreateReleaseCommand();
        command.setNamespace("default");
        command.setProjectName("game");
        return command;
    }
}
