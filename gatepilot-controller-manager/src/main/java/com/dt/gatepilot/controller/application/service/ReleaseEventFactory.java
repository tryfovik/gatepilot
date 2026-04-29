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

import com.dt.gatepilot.domain.enums.EventSeverity;
import com.dt.gatepilot.domain.enums.ResourceKind;
import com.dt.gatepilot.domain.resource.event.GatewayEventConstants;
import com.dt.gatepilot.domain.resource.event.GatewayEvent;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadataConstants;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
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
        // 事件名跟版本绑定，便于按发布版本排查
        event.getMetadata().setNamespace(publishedConfig.getMetadata().getNamespace());
        event.getMetadata().setName(ReleaseEventFactoryConstants.EVENT_NAME_PREFIX
                + publishedConfig.getSpec().getVersion());
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_EVENT_TYPE,
                GatewayEventConstants.EVENT_TYPE_PUBLISHED_CONFIG_GENERATED);
        event.getMetadata().getLabels().put(ResourceMetadataConstants.LABEL_VERSION,
                publishedConfig.getSpec().getVersion());
        GatewayEvent.GatewayEventSpec spec = event.getSpec();
        spec.setSeverity(EventSeverity.INFO);
        spec.setSource(GatewayEventConstants.SOURCE_CONTROLLER_MANAGER);
        spec.setReason(GatewayEventConstants.REASON_PUBLISHED_CONFIG_GENERATED);
        spec.setMessage(ReleaseEventFactoryConstants.MESSAGE_PUBLISHED_CONFIG_GENERATED);
        spec.setFirstObservedAt(Instant.now());
        spec.setLastObservedAt(spec.getFirstObservedAt());
        spec.setCount(1);
        ResourceReference involvedObject = new ResourceReference();
        // involvedObject 指向刚生成的 PublishedConfig
        involvedObject.setKind(ResourceKind.PUBLISHED_CONFIG);
        involvedObject.setNamespace(publishedConfig.getMetadata().getNamespace());
        involvedObject.setName(publishedConfig.getMetadata().getName());
        involvedObject.setUid(publishedConfig.getMetadata().getUid());
        spec.setInvolvedObject(involvedObject);
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_VERSION, publishedConfig.getSpec().getVersion());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_CONFIG_HASH,
                publishedConfig.getSpec().getConfigHash());
        spec.getAttributes().put(GatewayEventConstants.ATTRIBUTE_CONFIG_SHARD,
                publishedConfig.getSpec().getConfigShard());
        return event;
    }

    /**
     * 创建回滚 PublishedConfig 已生成事件。
     *
     * @param publishedConfig 回滚发布配置
     * @param sourceConfig 回滚来源配置
     * @return 网关事件
     */
    public GatewayEvent rollbackConfigGenerated(PublishedConfig publishedConfig, PublishedConfig sourceConfig) {
        GatewayEvent event = publishedConfigGenerated(publishedConfig);
        // 回滚事件补充来源版本，方便控制台串联回滚链路
        event.getSpec().setReason(GatewayEventConstants.REASON_ROLLBACK_CONFIG_GENERATED);
        event.getSpec().setMessage(ReleaseEventFactoryConstants.MESSAGE_ROLLBACK_CONFIG_GENERATED);
        event.getSpec().getAttributes().put(GatewayEventConstants.ATTRIBUTE_TARGET_VERSION,
                sourceConfig.getSpec().getVersion());
        return event;
    }
}
