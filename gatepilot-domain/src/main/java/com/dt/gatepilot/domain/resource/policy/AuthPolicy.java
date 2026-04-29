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
package com.dt.gatepilot.domain.resource.policy;

import com.dt.gatepilot.domain.enums.AuthType;
import com.dt.gatepilot.domain.resource.meta.LabelSelector;
import com.dt.gatepilot.domain.resource.meta.ResourceMetadata;
import com.dt.gatepilot.domain.resource.meta.ResourceReference;
import com.dt.gatepilot.domain.resource.meta.ResourceStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 认证策略，描述路由或项目级认证要求。
 */
@Data
public class AuthPolicy {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private AuthPolicySpec spec = new AuthPolicySpec();

    /**
     * 当前状态。
     */
    private AuthPolicyStatus status = new AuthPolicyStatus();

    /**
     * 认证策略期望状态。
     */
    @Data
    public static class AuthPolicySpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 认证类型。
         */
        private AuthType type;

        /**
         * 显式绑定的目标资源。
         */
        private List<ResourceReference> targetRefs = new ArrayList<>();

        /**
         * 按标签绑定的目标资源。
         */
        private LabelSelector targetSelector;

        /**
         * 是否允许匿名访问。
         */
        private Boolean anonymousAllowed;

        /**
         * 凭据引用，例如密钥资源或外部密钥名称。
         */
        private List<String> credentialRefs = new ArrayList<>();

        /**
         * JWT 配置。
         */
        private JwtConfig jwt = new JwtConfig();

        /**
         * API Key 配置。
         */
        private ApiKeyConfig apiKey = new ApiKeyConfig();
    }

    /**
     * JWT 认证配置。
     */
    @Data
    public static class JwtConfig {

        /**
         * 签发方。
         */
        private String issuer;

        /**
         * 受众。
         */
        private List<String> audiences = new ArrayList<>();

        /**
         * JWKS 地址。
         */
        private String jwksUri;

        /**
         * Token 时钟偏移容忍时间。
         */
        private Duration clockSkew;

        /**
         * Claim 到请求头的映射。
         */
        private Map<String, String> claimToHeader = new LinkedHashMap<>();
    }

    /**
     * API Key 认证配置。
     */
    @Data
    public static class ApiKeyConfig {

        /**
         * API Key 来源，例如 header 或 query。
         */
        private String source;

        /**
         * API Key 字段名。
         */
        private String name;

        /**
         * 是否把凭据透传给上游。
         */
        private Boolean forwardToUpstream;
    }

    /**
     * 认证策略实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AuthPolicyStatus extends ResourceStatus {

        /**
         * 当前绑定到的资源数量。
         */
        private Integer boundResourceCount;

        /**
         * 当前生效的发布版本。
         */
        private String currentPublishedVersion;
    }
}
