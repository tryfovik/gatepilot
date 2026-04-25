package com.dt.gatepilot.domain.resource.route;

import com.dt.gatepilot.domain.enums.HttpMethod;
import com.dt.gatepilot.domain.enums.Protocol;
import com.dt.gatepilot.domain.resource.common.ResourceMetadata;
import com.dt.gatepilot.domain.resource.common.ResourceReference;
import com.dt.gatepilot.domain.resource.common.ResourceStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 网关路由资源，用于描述入口匹配和上游转发关系。
 */
@Data
public class GatewayRoute {

    /**
     * 资源元信息。
     */
    private ResourceMetadata metadata = new ResourceMetadata();

    /**
     * 期望状态。
     */
    private GatewayRouteSpec spec = new GatewayRouteSpec();

    /**
     * 当前状态。
     */
    private GatewayRouteStatus status = new GatewayRouteStatus();

    /**
     * 网关路由期望状态。
     */
    @Data
    public static class GatewayRouteSpec {

        /**
         * 所属项目。
         */
        private ResourceReference projectRef;

        /**
         * 入口协议。
         */
        private List<Protocol> protocols = new ArrayList<>();

        /**
         * 入口域名。
         */
        private List<String> hosts = new ArrayList<>();

        /**
         * 路径匹配规则。
         */
        private RoutePathMatch path = new RoutePathMatch();

        /**
         * HTTP 方法匹配。
         */
        private List<HttpMethod> methods = new ArrayList<>();

        /**
         * 默认上游。
         */
        private ResourceReference upstreamRef;

        /**
         * 绑定到当前路由的策略。
         */
        private List<ResourceReference> policyRefs = new ArrayList<>();

        /**
         * 请求重写规则。
         */
        private RewriteRule rewrite = new RewriteRule();

        /**
         * 自定义扩展字段。
         */
        private Map<String, String> extensions = new LinkedHashMap<>();
    }

    /**
     * 路径匹配规则。
     */
    @Data
    public static class RoutePathMatch {

        /**
         * 匹配类型，例如 Prefix、Exact、Regex。
         */
        private String type;

        /**
         * 匹配表达式。
         */
        private String value;

        /**
         * 是否去除匹配到的前缀。
         */
        private Boolean stripPrefix;
    }

    /**
     * 请求重写规则。
     */
    @Data
    public static class RewriteRule {

        /**
         * 重写后的路径前缀。
         */
        private String pathPrefix;

        /**
         * 追加到上游请求的请求头。
         */
        private Map<String, String> addHeaders = new LinkedHashMap<>();

        /**
         * 转发前需要移除的请求头。
         */
        private List<String> removeHeaders = new ArrayList<>();
    }

    /**
     * 网关路由实际状态。
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GatewayRouteStatus extends ResourceStatus {

        /**
         * 当前生效的发布版本。
         */
        private String currentPublishedVersion;

        /**
         * 最近一次通过 dry-run 校验的版本。
         */
        private String lastValidatedVersion;

        /**
         * 最近一次应用失败的节点数。
         */
        private Integer failedNodeCount;
    }
}
