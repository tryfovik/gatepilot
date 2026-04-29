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
package com.dt.gatepilot.proxy.infrastructure.health;

import com.dt.gatepilot.proxy.domain.runtime.ProxyUpstreamHealthConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * proxy 上游健康探测调度器。
 */
@Slf4j
public class UpstreamHealthProbeScheduler {

    /**
     * 上游健康探测器。
     */
    private final UpstreamHealthProbe probe;

    /**
     * 创建上游健康探测调度器。
     *
     * @param probe 上游健康探测器
     */
    public UpstreamHealthProbeScheduler(UpstreamHealthProbe probe) {
        this.probe = probe;
    }

    /**
     * 定时执行上游健康探测。
     */
    @Scheduled(
            fixedDelayString = ProxyUpstreamHealthConstants.INTERVAL_PLACEHOLDER,
            initialDelayString = ProxyUpstreamHealthConstants.INITIAL_DELAY_PLACEHOLDER
    )
    public void probe() {
        // 探测失败只影响本轮健康视图，不影响业务请求线程
        probe.probe().subscribe(null, exception -> log.warn("GatePilot upstream health probe failed: {}",
                exception.getMessage(), exception));
    }
}
