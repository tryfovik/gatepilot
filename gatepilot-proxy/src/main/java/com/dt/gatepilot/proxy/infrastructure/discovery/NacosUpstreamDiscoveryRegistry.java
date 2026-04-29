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
package com.dt.gatepilot.proxy.infrastructure.discovery;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.dt.gatepilot.domain.enums.RegistryAuthType;
import com.dt.gatepilot.domain.enums.RegistryCenterType;
import com.dt.gatepilot.domain.enums.UpstreamDiscoveryType;
import com.dt.gatepilot.proxy.domain.port.UpstreamDiscoveryRegistry;
import com.dt.gatepilot.proxy.domain.runtime.CompiledProxyRuntime;
import com.dt.gatepilot.proxy.domain.runtime.CompiledUpstream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.StringUtils;

/**
 * 基于 Nacos 的上游实例发现注册表。
 */
public class NacosUpstreamDiscoveryRegistry implements UpstreamDiscoveryRegistry, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NacosUpstreamDiscoveryRegistry.class);

    /**
     * Nacos 客户端缓存。
     */
    private final ConcurrentMap<String, NamingService> clients = new ConcurrentHashMap<>();

    /**
     * Nacos 订阅缓存。
     */
    private final ConcurrentMap<String, NacosSubscription> subscriptions = new ConcurrentHashMap<>();

    /**
     * 上游到订阅 key 的索引。
     */
    private final ConcurrentMap<String, String> subscriptionKeysByUpstream = new ConcurrentHashMap<>();

    /**
     * 上游当前实例快照。
     */
    private final ConcurrentMap<String, List<CompiledUpstream.CompiledEndpoint>> endpointsByUpstream =
            new ConcurrentHashMap<>();

    /**
     * 根据新运行态刷新订阅。
     *
     * @param runtime 新运行态
     */
    @Override
    public void refresh(CompiledProxyRuntime runtime) {
        if (runtime == null) {
            return;
        }
        Set<String> desiredKeys = new LinkedHashSet<>();
        for (CompiledUpstream upstream : runtime.getUpstreamsByName().values()) {
            if (!nacos(upstream)) {
                endpointsByUpstream.remove(upstream.getName());
                subscriptionKeysByUpstream.remove(upstream.getName());
                continue;
            }
            String key = subscriptionKey(upstream);
            desiredKeys.add(key);
            subscriptionKeysByUpstream.put(upstream.getName(), key);
            NacosSubscription subscription = subscriptions.computeIfAbsent(key, ignored -> subscribe(upstream, key));
            if (subscription.namingService() == null) {
                subscriptions.remove(key, subscription);
            }
        }
        removeStaleSubscriptions(desiredKeys);
    }

    /**
     * 查询上游当前实例。
     *
     * @param upstream 已编译上游
     * @return 当前可用实例
     */
    @Override
    public List<CompiledUpstream.CompiledEndpoint> instances(CompiledUpstream upstream) {
        if (!nacos(upstream)) {
            return upstream.getEndpoints();
        }
        List<CompiledUpstream.CompiledEndpoint> endpoints = endpointsByUpstream.get(upstream.getName());
        if (endpoints == null || endpoints.isEmpty()) {
            // 首次订阅失败时允许静态兜底端点继续工作
            return upstream.getEndpoints();
        }
        return endpoints;
    }

    private NacosSubscription subscribe(CompiledUpstream upstream, String key) {
        CompiledUpstream.CompiledDiscovery discovery = upstream.getDiscovery();
        try {
            NamingService namingService = client(discovery);
            EventListener listener = event -> {
                if (event instanceof NamingEvent namingEvent) {
                    endpointsByUpstream.put(upstream.getName(), endpoints(discovery, namingEvent.getInstances()));
                }
            };
            NacosSubscription subscription = new NacosSubscription(key, upstream.getName(), namingService,
                    discovery, listener);
            refreshInstances(subscription);
            subscribe(subscription);
            return subscription;
        } catch (NacosException exception) {
            log.warn("GatePilot Nacos upstream subscribe failed, upstream={}, service={}",
                    upstream.getName(), discovery.getServiceName(), exception);
            return new NacosSubscription(key, upstream.getName(), null, discovery, event -> {
            });
        }
    }

    private void refreshInstances(NacosSubscription subscription) throws NacosException {
        CompiledUpstream.CompiledDiscovery discovery = subscription.discovery();
        List<Instance> instances;
        if (discovery.getClusters().isEmpty()) {
            instances = subscription.namingService().selectInstances(discovery.getServiceName(),
                    discovery.getGroup(), healthyOnly(discovery));
        } else {
            instances = subscription.namingService().selectInstances(discovery.getServiceName(),
                    discovery.getGroup(), discovery.getClusters(), healthyOnly(discovery));
        }
        endpointsByUpstream.put(subscription.upstreamName(), endpoints(discovery, instances));
    }

    private void subscribe(NacosSubscription subscription) throws NacosException {
        CompiledUpstream.CompiledDiscovery discovery = subscription.discovery();
        if (discovery.getClusters().isEmpty()) {
            subscription.namingService().subscribe(discovery.getServiceName(), discovery.getGroup(),
                    subscription.listener());
            return;
        }
        subscription.namingService().subscribe(discovery.getServiceName(), discovery.getGroup(),
                discovery.getClusters(), subscription.listener());
    }

    private NamingService client(CompiledUpstream.CompiledDiscovery discovery) throws NacosException {
        String key = clientKey(discovery);
        try {
            return clients.computeIfAbsent(key, ignored -> {
                try {
                    return NacosFactory.createNamingService(properties(discovery));
                } catch (NacosException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        } catch (IllegalStateException exception) {
            if (exception.getCause() instanceof NacosException nacosException) {
                throw nacosException;
            }
            throw exception;
        }
    }

    private Properties properties(CompiledUpstream.CompiledDiscovery discovery) {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, discovery.getServerAddr());
        if (StringUtils.hasText(discovery.getNamespace())) {
            properties.put(PropertyKeyConst.NAMESPACE, discovery.getNamespace());
        }
        if (discovery.getAuthType() == RegistryAuthType.USERNAME_PASSWORD) {
            putIfText(properties, PropertyKeyConst.USERNAME, discovery.getUsername());
            putIfText(properties, PropertyKeyConst.PASSWORD, discovery.getPassword());
        }
        if (discovery.getAuthType() == RegistryAuthType.AKSK) {
            putIfText(properties, PropertyKeyConst.ACCESS_KEY, discovery.getAccessKey());
            putIfText(properties, PropertyKeyConst.SECRET_KEY, discovery.getSecretKey());
        }
        return properties;
    }

    private void putIfText(Properties properties, String key, String value) {
        if (StringUtils.hasText(value)) {
            properties.put(key, value);
        }
    }

    private List<CompiledUpstream.CompiledEndpoint> endpoints(CompiledUpstream.CompiledDiscovery discovery,
                                                              List<Instance> instances) {
        if (instances == null || instances.isEmpty()) {
            return List.of();
        }
        List<CompiledUpstream.CompiledEndpoint> endpoints = new ArrayList<>();
        instances.stream()
                .filter(Objects::nonNull)
                .filter(instance -> StringUtils.hasText(instance.getIp()))
                .filter(instance -> instance.getPort() > 0)
                .filter(instance -> !enabledOnly(discovery) || instance.isEnabled())
                .filter(instance -> !healthyOnly(discovery) || instance.isHealthy())
                .filter(instance -> metadataMatches(discovery, instance))
                .sorted(Comparator.comparing(Instance::getIp).thenComparingInt(Instance::getPort))
                .map(instance -> endpoint(discovery, instance))
                .forEach(endpoints::add);
        return List.copyOf(endpoints);
    }

    private CompiledUpstream.CompiledEndpoint endpoint(CompiledUpstream.CompiledDiscovery discovery,
                                                       Instance instance) {
        CompiledUpstream.CompiledEndpoint endpoint = new CompiledUpstream.CompiledEndpoint();
        endpoint.setHost(instance.getIp());
        endpoint.setPort(instance.getPort());
        endpoint.setWeight(weight(instance));
        Map<String, String> labels = new LinkedHashMap<>();
        if (instance.getMetadata() != null) {
            labels.putAll(instance.getMetadata());
        }
        putIfText(labels, NacosUpstreamDiscoveryConstants.LABEL_NACOS_SERVICE, discovery.getServiceName());
        putIfText(labels, NacosUpstreamDiscoveryConstants.LABEL_NACOS_CLUSTER, instance.getClusterName());
        putIfText(labels, NacosUpstreamDiscoveryConstants.LABEL_NACOS_INSTANCE_ID, instance.getInstanceId());
        endpoint.setLabels(labels);
        return endpoint;
    }

    private void putIfText(Map<String, String> labels, String key, String value) {
        if (StringUtils.hasText(value)) {
            labels.put(key, value);
        }
    }

    private boolean metadataMatches(CompiledUpstream.CompiledDiscovery discovery, Instance instance) {
        Map<String, String> selector = discovery.getMetadataSelector();
        if (selector == null || selector.isEmpty()) {
            return true;
        }
        Map<String, String> metadata = instance.getMetadata() == null ? Map.of() : instance.getMetadata();
        for (Map.Entry<String, String> entry : selector.entrySet()) {
            if (!Objects.equals(metadata.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private int weight(Instance instance) {
        long rounded = Math.round(instance.getWeight());
        return rounded <= 0 ? 1 : (int) Math.min(Integer.MAX_VALUE, rounded);
    }

    private void removeStaleSubscriptions(Set<String> desiredKeys) {
        List<String> staleKeys = subscriptions.keySet().stream()
                .filter(key -> !desiredKeys.contains(key))
                .toList();
        for (String key : staleKeys) {
            NacosSubscription subscription = subscriptions.remove(key);
            if (subscription == null) {
                continue;
            }
            unsubscribe(subscription);
            subscriptionKeysByUpstream.remove(subscription.upstreamName(), key);
            endpointsByUpstream.remove(subscription.upstreamName());
        }
    }

    private void unsubscribe(NacosSubscription subscription) {
        if (subscription.namingService() == null) {
            return;
        }
        try {
            CompiledUpstream.CompiledDiscovery discovery = subscription.discovery();
            if (discovery.getClusters().isEmpty()) {
                subscription.namingService().unsubscribe(discovery.getServiceName(), discovery.getGroup(),
                        subscription.listener());
            } else {
                subscription.namingService().unsubscribe(discovery.getServiceName(), discovery.getGroup(),
                        discovery.getClusters(), subscription.listener());
            }
        } catch (NacosException exception) {
            log.warn("GatePilot Nacos upstream unsubscribe failed, upstream={}", subscription.upstreamName(),
                    exception);
        }
    }

    private boolean nacos(CompiledUpstream upstream) {
        CompiledUpstream.CompiledDiscovery discovery = upstream.getDiscovery();
        return discovery != null
                && discovery.getType() == UpstreamDiscoveryType.NACOS
                && discovery.getRegistryType() == RegistryCenterType.NACOS
                && StringUtils.hasText(discovery.getServerAddr())
                && StringUtils.hasText(discovery.getGroup())
                && StringUtils.hasText(discovery.getServiceName());
    }

    private boolean healthyOnly(CompiledUpstream.CompiledDiscovery discovery) {
        return !Boolean.FALSE.equals(discovery.getHealthyOnly());
    }

    private boolean enabledOnly(CompiledUpstream.CompiledDiscovery discovery) {
        return !Boolean.FALSE.equals(discovery.getEnabledOnly());
    }

    private String subscriptionKey(CompiledUpstream upstream) {
        CompiledUpstream.CompiledDiscovery discovery = upstream.getDiscovery();
        return String.join(NacosUpstreamDiscoveryConstants.KEY_SEPARATOR,
                upstream.getName(),
                safe(discovery.getServerAddr()),
                safe(discovery.getNamespace()),
                safe(discovery.getGroup()),
                safe(discovery.getServiceName()),
                String.join(NacosUpstreamDiscoveryConstants.CLUSTER_SEPARATOR, discovery.getClusters()),
                metadataKey(discovery.getMetadataSelector()),
                Boolean.toString(healthyOnly(discovery)),
                Boolean.toString(enabledOnly(discovery)),
                Integer.toHexString(Objects.hash(discovery.getAuthType(), discovery.getUsername(),
                        discovery.getPassword(), discovery.getAccessKey(), discovery.getSecretKey())));
    }

    private String clientKey(CompiledUpstream.CompiledDiscovery discovery) {
        return String.join(NacosUpstreamDiscoveryConstants.KEY_SEPARATOR,
                safe(discovery.getServerAddr()),
                safe(discovery.getNamespace()),
                Integer.toHexString(Objects.hash(discovery.getAuthType(), discovery.getUsername(),
                        discovery.getPassword(), discovery.getAccessKey(), discovery.getSecretKey())));
    }

    private String metadataKey(Map<String, String> metadataSelector) {
        if (metadataSelector == null || metadataSelector.isEmpty()) {
            return NacosUpstreamDiscoveryConstants.EMPTY_PART;
        }
        return metadataSelector.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey()
                        + NacosUpstreamDiscoveryConstants.METADATA_KEY_VALUE_SEPARATOR
                        + entry.getValue())
                .reduce((left, right) -> left
                        + NacosUpstreamDiscoveryConstants.METADATA_SEPARATOR
                        + right)
                .orElse(NacosUpstreamDiscoveryConstants.EMPTY_PART);
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : NacosUpstreamDiscoveryConstants.EMPTY_PART;
    }

    /**
     * 关闭 Nacos 客户端。
     */
    @Override
    public void destroy() {
        subscriptions.values().forEach(this::unsubscribe);
        subscriptions.clear();
        clients.values().forEach(client -> {
            try {
                client.shutDown();
            } catch (NacosException exception) {
                log.warn("GatePilot Nacos client shutdown failed", exception);
            }
        });
        clients.clear();
        endpointsByUpstream.clear();
        subscriptionKeysByUpstream.clear();
    }

    private record NacosSubscription(String key,
                                     String upstreamName,
                                     NamingService namingService,
                                     CompiledUpstream.CompiledDiscovery discovery,
                                     EventListener listener) {
    }
}
