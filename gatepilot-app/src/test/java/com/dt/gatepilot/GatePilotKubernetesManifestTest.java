package com.dt.gatepilot;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GatePilot Kubernetes 基础清单测试
 */
class GatePilotKubernetesManifestTest {

    private static final String MANIFEST_PATH = "deploy/kubernetes/base/gatepilot.yaml";

    private static final String KIND_DEPLOYMENT = "Deployment";

    private static final String KIND_SERVICE = "Service";

    private static final String KIND_HPA = "HorizontalPodAutoscaler";

    private static final String KIND_PDB = "PodDisruptionBudget";

    private static final String KIND_CONFIG_MAP = "ConfigMap";

    private static final String NAME_PROXY = "gatepilot-proxy";

    private static final String NAME_APISERVER = "gatepilot-apiserver";

    private static final String NAME_APISERVER_CONFIG = "gatepilot-apiserver-config";

    private static final String KEY_KIND = "kind";

    private static final String KEY_METADATA = "metadata";

    private static final String KEY_NAME = "name";

    private static final String KEY_SPEC = "spec";

    private static final String KEY_SELECTOR = "selector";

    private static final String KEY_MATCH_LABELS = "matchLabels";

    private static final String KEY_SCALE_TARGET_REF = "scaleTargetRef";

    private static final String KEY_REPLICAS = "replicas";

    private static final String KEY_DATA = "data";

    private static final String KEY_APPLICATION_YAML = "application.yaml";

    private static final String KEY_GETBOOT = "getboot";

    private static final String KEY_LOCK = "lock";

    private static final String KEY_TYPE = "type";

    private static final String KEY_DATABASE = "database";

    private static final String KEY_ENABLED = "enabled";

    private static final String KEY_INITIALIZE_SCHEMA = "initialize-schema";

    private static final String KEY_GATEPILOT = "gatepilot";

    private static final String KEY_CONTROLLER_MANAGER = "controller-manager";

    private static final String KEY_DISTRIBUTED_LOCK_REQUIRED = "distributed-lock-required";

    private static final String LOCK_TYPE_DATABASE = "database";

    private static final int MIN_PROXY_REPLICAS = 3;

    @Test
    void shouldKeepKubernetesBoundaryAndProxyScaleOutResources() throws IOException {
        List<Map<String, Object>> resources = loadResources();
        Map<String, Object> apiserverConfig = applicationConfig(
                requireResource(resources, KIND_CONFIG_MAP, NAME_APISERVER_CONFIG));
        Map<String, Object> proxyDeployment = requireResource(resources, KIND_DEPLOYMENT, NAME_PROXY);
        Map<String, Object> proxyService = requireResource(resources, KIND_SERVICE, NAME_PROXY);
        Map<String, Object> proxyHpa = requireResource(resources, KIND_HPA, NAME_PROXY);
        Map<String, Object> proxyPdb = requireResource(resources, KIND_PDB, NAME_PROXY);

        assertThat(resourceNames(resources, KIND_DEPLOYMENT))
                .contains(NAME_APISERVER, NAME_PROXY)
                .doesNotContain("nginx", "haproxy", "keepalived");
        assertThat(resourceNames(resources, KIND_SERVICE))
                .contains(NAME_APISERVER, NAME_PROXY)
                .doesNotContain("nginx", "haproxy", "keepalived");
        assertThat(numberAt(proxyDeployment, KEY_SPEC, KEY_REPLICAS).intValue())
                .isGreaterThanOrEqualTo(MIN_PROXY_REPLICAS);
        assertThat(mapAt(proxyService, KEY_SPEC, KEY_SELECTOR))
                .containsAllEntriesOf(mapAt(proxyDeployment, KEY_SPEC, KEY_SELECTOR, KEY_MATCH_LABELS));
        assertThat(stringAt(proxyHpa, KEY_SPEC, KEY_SCALE_TARGET_REF, KEY_NAME)).isEqualTo(NAME_PROXY);
        assertThat(mapAt(proxyPdb, KEY_SPEC, KEY_SELECTOR, KEY_MATCH_LABELS))
                .containsAllEntriesOf(mapAt(proxyDeployment, KEY_SPEC, KEY_SELECTOR, KEY_MATCH_LABELS));
        assertThat(stringAt(apiserverConfig, KEY_GETBOOT, KEY_LOCK, KEY_TYPE)).isEqualTo(LOCK_TYPE_DATABASE);
        assertThat(booleanAt(apiserverConfig, KEY_GETBOOT, KEY_LOCK, KEY_DATABASE, KEY_ENABLED)).isTrue();
        assertThat(booleanAt(apiserverConfig, KEY_GETBOOT, KEY_LOCK, KEY_DATABASE, KEY_INITIALIZE_SCHEMA)).isTrue();
        assertThat(booleanAt(apiserverConfig, KEY_GATEPILOT, KEY_CONTROLLER_MANAGER,
                KEY_DISTRIBUTED_LOCK_REQUIRED)).isTrue();
    }

    private List<Map<String, Object>> loadResources() throws IOException {
        Path path = manifestPath();
        try (Reader reader = Files.newBufferedReader(path)) {
            Yaml yaml = new Yaml();
            List<Map<String, Object>> resources = new ArrayList<>();
            // YAML 多文档按资源列表读取，空文档直接过滤
            for (Object item : yaml.loadAll(reader)) {
                if (item instanceof Map<?, ?> resource) {
                    resources.add((Map<String, Object>) resource);
                }
            }
            return resources;
        }
    }

    private Path manifestPath() {
        Path current = Path.of("").toAbsolutePath();
        Path rootManifest = current.resolve(MANIFEST_PATH);
        if (Files.exists(rootManifest)) {
            return rootManifest;
        }
        return current.getParent().resolve(MANIFEST_PATH);
    }

    private Map<String, Object> requireResource(List<Map<String, Object>> resources, String kind, String name) {
        return resources.stream()
                .filter(resource -> Objects.equals(kind, resource.get(KEY_KIND)))
                .filter(resource -> Objects.equals(name, stringAt(resource, KEY_METADATA, KEY_NAME)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(kind + "/" + name + " missing"));
    }

    private List<String> resourceNames(List<Map<String, Object>> resources, String kind) {
        return resources.stream()
                .filter(resource -> Objects.equals(kind, resource.get(KEY_KIND)))
                .map(resource -> stringAt(resource, KEY_METADATA, KEY_NAME))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> applicationConfig(Map<String, Object> configMap) {
        String content = stringAt(configMap, KEY_DATA, KEY_APPLICATION_YAML);
        return (Map<String, Object>) new Yaml().load(content);
    }

    private Number numberAt(Map<String, Object> source, String... keys) {
        return (Number) valueAt(source, keys);
    }

    private Boolean booleanAt(Map<String, Object> source, String... keys) {
        return (Boolean) valueAt(source, keys);
    }

    private String stringAt(Map<String, Object> source, String... keys) {
        return Objects.toString(valueAt(source, keys), null);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapAt(Map<String, Object> source, String... keys) {
        return (Map<String, Object>) valueAt(source, keys);
    }

    @SuppressWarnings("unchecked")
    private Object valueAt(Map<String, Object> source, String... keys) {
        Map<String, Object> current = new LinkedHashMap<>(source);
        for (int index = 0; index < keys.length; index++) {
            Object value = current.get(keys[index]);
            if (index == keys.length - 1) {
                return value;
            }
            current = (Map<String, Object>) value;
        }
        return null;
    }
}
