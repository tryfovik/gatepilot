package com.dt.platform.gateway.infrastructure.audit;

import java.net.URI;
import java.time.Instant;

/**
 * 网关访问审计事件。
 */
public record GatewayAccessAuditEvent(Instant timestamp,
                                      String traceId,
                                      String clientIp,
                                      String method,
                                      String path,
                                      String routeType,
                                      String projectKey,
                                      String routeKey,
                                      Integer status,
                                      long latencyMs,
                                      String trafficColor,
                                      String releaseVariant,
                                      URI upstreamUri,
                                      boolean methodAllowed,
                                      boolean authRequired,
                                      boolean publicPath,
                                      boolean fallback,
                                      String outcome,
                                      String error) {
}
