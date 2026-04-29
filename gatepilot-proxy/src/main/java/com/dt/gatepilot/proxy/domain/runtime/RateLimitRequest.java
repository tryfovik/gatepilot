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

import java.util.Locale;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * 限流请求上下文。
 */
@RequiredArgsConstructor
public class RateLimitRequest {

    /**
     * 请求 Host。
     */
    private final String host;

    /**
     * Header 读取器。
     */
    private final Function<String, String> headerValueReader;

    /**
     * Cookie 读取器。
     */
    private final Function<String, String> cookieValueReader;

    /**
     * Query 读取器。
     */
    private final Function<String, String> queryValueReader;

    /**
     * 客户端地址。
     */
    private final String remoteAddress;

    /**
     * 读取限流参数值。
     *
     * @param source 参数来源
     * @param name 参数名称
     * @return 参数值
     */
    public String value(String source, String name) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSource) {
            case ProxyRateLimitConstants.SOURCE_HEADER -> read(headerValueReader, name);
            case ProxyRateLimitConstants.SOURCE_QUERY, ProxyRateLimitConstants.SOURCE_URL_PARAM ->
                    read(queryValueReader, name);
            case ProxyRateLimitConstants.SOURCE_COOKIE -> read(cookieValueReader, name);
            case ProxyRateLimitConstants.SOURCE_IP, ProxyRateLimitConstants.SOURCE_CLIENT_IP -> remoteAddress;
            case ProxyRateLimitConstants.SOURCE_HOST -> host;
            default -> null;
        };
    }

    /**
     * 读取参数值。
     *
     * @param reader 参数读取器
     * @param name 参数名称
     * @return 参数值
     */
    private String read(Function<String, String> reader, String name) {
        if (reader == null || !StringUtils.hasText(name)) {
            return null;
        }
        return reader.apply(name);
    }
}
