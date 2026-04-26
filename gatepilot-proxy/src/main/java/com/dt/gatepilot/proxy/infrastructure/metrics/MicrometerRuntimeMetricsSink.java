package com.dt.gatepilot.proxy.infrastructure.metrics;

import com.dt.gatepilot.proxy.domain.port.RuntimeMetricsSink;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.springframework.util.StringUtils;

/**
 * 基于 Micrometer 的 proxy 运行指标采集器。
 */
public class MicrometerRuntimeMetricsSink implements RuntimeMetricsSink {

    /**
     * Micrometer 注册表。
     */
    private final MeterRegistry meterRegistry;

    /**
     * 路由指标缓存。
     */
    private final ConcurrentMap<MetricsKey, RouteMeters> routeMeters = new ConcurrentHashMap<>();

    /**
     * 创建 Micrometer 指标采集器。
     *
     * @param meterRegistry Micrometer 注册表
     */
    public MicrometerRuntimeMetricsSink(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    /**
     * 记录路由请求指标。
     *
     * @param routeId 路由标识
     * @param status 响应状态码
     * @param latencyMillis 延迟
     */
    @Override
    public void recordRouteRequest(String routeId, int status, long latencyMillis) {
        MetricsKey key = new MetricsKey(normalizeRouteId(routeId),
                Integer.toString(status), statusClass(status));
        RouteMeters meters = routeMeters.computeIfAbsent(key, this::newRouteMeters);
        meters.requests().increment();
        meters.latency().record(Math.max(latencyMillis, 0L), TimeUnit.MILLISECONDS);
    }

    private RouteMeters newRouteMeters(MetricsKey key) {
        // meter 按标签缓存，避免高流量下反复构造同一组指标
        Counter requests = Counter.builder(ProxyMetricsConstants.ROUTE_REQUESTS_METRIC)
                .description(ProxyMetricsConstants.ROUTE_REQUESTS_DESCRIPTION)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, key.routeId())
                .tag(ProxyMetricsConstants.TAG_STATUS, key.status())
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, key.statusClass())
                .register(meterRegistry);
        Timer latency = Timer.builder(ProxyMetricsConstants.ROUTE_LATENCY_METRIC)
                .description(ProxyMetricsConstants.ROUTE_LATENCY_DESCRIPTION)
                .tag(ProxyMetricsConstants.TAG_ROUTE_ID, key.routeId())
                .tag(ProxyMetricsConstants.TAG_STATUS, key.status())
                .tag(ProxyMetricsConstants.TAG_STATUS_CLASS, key.statusClass())
                .register(meterRegistry);
        return new RouteMeters(requests, latency);
    }

    private String normalizeRouteId(String routeId) {
        if (!StringUtils.hasText(routeId)) {
            return ProxyMetricsConstants.TAG_UNKNOWN;
        }
        return routeId;
    }

    private String statusClass(int status) {
        if (status < ProxyMetricsConstants.MIN_HTTP_STATUS) {
            return ProxyMetricsConstants.TAG_UNKNOWN;
        }
        return (status / ProxyMetricsConstants.STATUS_CLASS_DIVISOR)
                + ProxyMetricsConstants.STATUS_CLASS_SUFFIX;
    }

    private record MetricsKey(String routeId, String status, String statusClass) {
    }

    private record RouteMeters(Counter requests, Timer latency) {
    }
}
