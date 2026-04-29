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
package com.dt.gatepilot.proxy.domain.runtime;

import com.dt.gatepilot.domain.enums.TrafficColorSource;
import com.dt.gatepilot.domain.resource.policy.TrafficPolicy;
import com.dt.gatepilot.domain.resource.publish.PublishedConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流量染色解析测试。
 */
class TrafficColorResolverTest {

    @Test
    void shouldTrustConfiguredRequestHeader() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of("X-Traffic-Color", "blue"),
                Map.of(),
                Map.of()
        ));

        assertThat(color).isEqualTo("blue");
    }

    @Test
    void shouldResolveColorFromPolicyRules() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of(),
                Map.of("beta", "true"),
                Map.of()
        ));

        assertThat(color).isEqualTo("green");
    }

    @Test
    void shouldResolveColorFromQueryRule() {
        CompiledProxyRuntime runtime = runtimeWithTrafficRules(queryRule());

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of(),
                Map.of(),
                Map.of("preview", "enabled")
        ));

        assertThat(color).isEqualTo("yellow");
    }

    @Test
    void shouldResolveColorFromIpRule() {
        CompiledProxyRuntime runtime = runtimeWithTrafficRules(ipRule());

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of(),
                Map.of(),
                Map.of(),
                "10.20.30.40"
        ));

        assertThat(color).isEqualTo("gray");
    }

    @Test
    void shouldResolveWeightedReleaseColorWhenNoRuleMatches() {
        CompiledProxyRuntime runtime = runtime();

        String color = new TrafficColorResolver().resolve(runtime, request(
                Map.of("X-User-Id", "u-10001"),
                Map.of(),
                Map.of()
        ));

        assertThat(color).isEqualTo("canary");
    }

    @Test
    void shouldReturnDefaultColorWhenNothingMatches() {
        CompiledProxyRuntime runtime = runtimeWithoutReleasePolicy();

        String color = new TrafficColorResolver().resolve(runtime, request(Map.of(), Map.of(), Map.of()));

        assertThat(color).isEqualTo("stable");
    }

    private CompiledProxyRuntime runtime() {
        PublishedConfig config = baseConfig();
        config.getSpec().getPolicies().add(trafficPolicy());
        config.getSpec().getPolicies().add(releasePolicy());
        return new PublishedConfigCompiler().compile(config);
    }

    private CompiledProxyRuntime runtimeWithoutReleasePolicy() {
        PublishedConfig config = baseConfig();
        config.getSpec().getPolicies().add(trafficPolicy());
        return new PublishedConfigCompiler().compile(config);
    }

    private CompiledProxyRuntime runtimeWithTrafficRules(TrafficPolicy.TrafficColorRule... rules) {
        PublishedConfig config = baseConfig();
        PublishedConfig.PublishedPolicy policy = trafficPolicy(List.of(rules));
        config.getSpec().getPolicies().add(policy);
        return new PublishedConfigCompiler().compile(config);
    }

    private PublishedConfig baseConfig() {
        PublishedConfig config = new PublishedConfig();
        config.getSpec().setVersion("v1");
        config.getSpec().setConfigHash("hash-v1");
        PublishedConfig.PublishedRoute route = new PublishedConfig.PublishedRoute();
        route.setRouteId("admin");
        route.getHosts().add("api.example.com");
        route.setPath("/api/game/admin");
        route.getPolicyNames().add("traffic-main");
        route.getPolicyNames().add("release-main");
        config.getSpec().getRoutes().add(route);
        return config;
    }

    private PublishedConfig.PublishedPolicy trafficPolicy() {
        return trafficPolicy(List.of(cookieRule()));
    }

    private PublishedConfig.PublishedPolicy trafficPolicy(List<TrafficPolicy.TrafficColorRule> rules) {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("traffic-main");
        policy.setType("TrafficPolicy");
        policy.getConfig().put("trustRequestHeader", true);
        policy.getConfig().put("defaultColor", "stable");
        policy.getConfig().put("colorRules", rules);
        return policy;
    }

    private TrafficPolicy.TrafficColorRule cookieRule() {
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.COOKIE);
        rule.setKey("beta");
        rule.setMatch("true");
        rule.setColor("green");
        return rule;
    }

    private TrafficPolicy.TrafficColorRule queryRule() {
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.QUERY);
        rule.setKey("preview");
        rule.setMatch("enabled");
        rule.setColor("yellow");
        return rule;
    }

    private TrafficPolicy.TrafficColorRule ipRule() {
        TrafficPolicy.TrafficColorRule rule = new TrafficPolicy.TrafficColorRule();
        rule.setSource(TrafficColorSource.IP);
        rule.setMatch("10.20.30.40");
        rule.setColor("gray");
        return rule;
    }

    private PublishedConfig.PublishedPolicy releasePolicy() {
        PublishedConfig.PublishedPolicy policy = new PublishedConfig.PublishedPolicy();
        policy.setName("release-main");
        policy.setType("ReleasePolicy");
        Map<String, Object> split = new LinkedHashMap<>();
        split.put("target", "canary");
        split.put("color", "canary");
        split.put("weight", 100);
        policy.getConfig().put("trafficSplits", List.of(split));
        return policy;
    }

    private TrafficColorRequest request(Map<String, String> headers,
                                        Map<String, String> cookies,
                                        Map<String, String> query) {
        return request(headers, cookies, query, "127.0.0.1");
    }

    private TrafficColorRequest request(Map<String, String> headers,
                                        Map<String, String> cookies,
                                        Map<String, String> query,
                                        String remoteAddress) {
        return new TrafficColorRequest(
                "api.example.com",
                "/api/game/admin/users",
                null,
                headers::get,
                cookies::get,
                query::get,
                remoteAddress
        );
    }
}
