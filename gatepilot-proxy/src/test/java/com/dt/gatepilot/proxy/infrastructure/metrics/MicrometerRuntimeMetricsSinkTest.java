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
package com.dt.gatepilot.proxy.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Micrometer 运行指标采集器测试。
 */
class MicrometerRuntimeMetricsSinkTest {

    @Test
    void shouldRecordRouteRequestMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerRuntimeMetricsSink sink = new MicrometerRuntimeMetricsSink(meterRegistry);

        sink.recordRouteRequest("admin-route", 200, 18L);

        Counter requests = meterRegistry.get(ProxyMetricsConstants.ROUTE_REQUESTS_METRIC)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, "admin-route")
                .tag(ProxyMetricsConstants.TAG_STATUS, "200")
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, "2xx")
                .counter();
        Timer latency = meterRegistry.get(ProxyMetricsConstants.ROUTE_LATENCY_METRIC)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, "admin-route")
                .tag(ProxyMetricsConstants.TAG_STATUS, "200")
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, "2xx")
                .timer();

        assertThat(requests.count()).isEqualTo(1.0D);
        assertThat(latency.count()).isEqualTo(1L);
        assertThat(latency.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(18.0D);
    }

    @Test
    void shouldNormalizeEmptyRouteAndNegativeLatency() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        MicrometerRuntimeMetricsSink sink = new MicrometerRuntimeMetricsSink(meterRegistry);

        sink.recordRouteRequest(null, 0, -1L);

        Counter requests = meterRegistry.get(ProxyMetricsConstants.ROUTE_REQUESTS_METRIC)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, ProxyMetricsConstants.TAG_UNKNOWN)
                .tag(ProxyMetricsConstants.TAG_STATUS, "0")
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, ProxyMetricsConstants.TAG_UNKNOWN)
                .counter();
        Timer latency = meterRegistry.get(ProxyMetricsConstants.ROUTE_LATENCY_METRIC)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, ProxyMetricsConstants.TAG_UNKNOWN)
                .tag(ProxyMetricsConstants.TAG_STATUS, "0")
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, ProxyMetricsConstants.TAG_UNKNOWN)
                .timer();

        assertThat(requests.count()).isEqualTo(1.0D);
        assertThat(latency.totalTime(TimeUnit.MILLISECONDS)).isZero();
    }
}
