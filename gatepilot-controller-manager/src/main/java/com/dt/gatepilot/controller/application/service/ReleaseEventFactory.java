package com.dt.gatepilot.controller.application.service;

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.time.Instant;

/**
 * 发布事件工厂。
 */
public class ReleaseEventFactory {

    /**
     * 创建 PublishedConfig 已生成事件。
     *
     * @param publishedConfig 已发布配置
     * @return 网关事件
     */
    public GatewayEvent publishedConfigGenerated(PublishedConfig publishedConfig) {
        GatewayEvent event = new GatewayEvent();
        event.getMetadata().setNamespace(publishedConfig.getMetadata().getNamespace());
        event.getMetadata().setName("event-" + publishedConfig.getSpec().getVersion());
        event.getMetadata().getLabels().put("gatepilot.io/event-type", "published-config-generated");
        event.getMetadata().getLabels().put("gatepilot.io/version", publishedConfig.getSpec().getVersion());
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.INFO);
        spec.setSource("controller-manager");
        spec.setReason("PublishedConfigGenerated");
        spec.setMessage("controller-manager 已生成 PublishedConfig");
        spec.setFirstObservedAt(Instant.now());
        spec.setLastObservedAt(spec.getFirstObservedAt());
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        involvedObject.setKind(ResourceKind.PUBLISHED_CONFIG);
        involvedObject.setNamespace(publishedConfig.getMetadata().getNamespace());
        involvedObject.setName(publishedConfig.getMetadata().getName());
        involvedObject.setUid(publishedConfig.getMetadata().getUid());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put("version", publishedConfig.getSpec().getVersion());
        spec.getAttributes().put("configHash", publishedConfig.getSpec().getConfigHash());
        spec.getAttributes().put("configShard", publishedConfig.getSpec().getConfigShard());
        return event;
    }
}
