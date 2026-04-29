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
package com.dt.gatepilot.demo.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dt.gatepilot.demo.application.dto.DemoEchoResponse;
import com.dt.gatepilot.demo.infrastructure.config.DemoUpstreamProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

/**
 * 示例上游回显服务测试
 */
class DemoEchoServiceTest {

    /**
     * 校验请求和关键请求头回显
     */
    @Test
    void shouldEchoRequestAndSelectedHeaders() {
        DemoUpstreamProperties properties = new DemoUpstreamProperties();
        properties.setVersion("green-v2");
        properties.setColor("green");
        DemoEchoService service = new DemoEchoService(properties,
                Clock.fixed(Instant.parse("2026-04-26T10:00:00Z"), ZoneOffset.UTC));

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/ping?name=gatepilot")
                .header(DemoEchoConstants.HEADER_TRACE_ID, "trace-1")
                .header(DemoEchoConstants.HEADER_TRAFFIC_COLOR, "canary")
                .header("Ignored-Header", "ignored")
                .build();

        DemoEchoResponse response = service.echo(request);

        assertThat(response.getVersion()).isEqualTo("green-v2");
        assertThat(response.getColor()).isEqualTo("green");
        assertThat(response.getPath()).isEqualTo("/ping");
        assertThat(response.getQuery()).isEqualTo("name=gatepilot");
        assertThat(response.getHeaders())
                .containsEntry(DemoEchoConstants.HEADER_TRACE_ID, "trace-1")
                .containsEntry(DemoEchoConstants.HEADER_TRAFFIC_COLOR, "canary")
                .doesNotContainKey("Ignored-Header");
        assertThat(response.getTimestamp()).isEqualTo(Instant.parse("2026-04-26T10:00:00Z"));
    }
}
