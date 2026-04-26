package com.dt.gatepilot.apiserver.application.service;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 项目接入模板常量
 */
public final class ProjectTemplateConstants {

    /**
     * 资源名安全字符正则
     */
    public static final Pattern SAFE_NAME_PATTERN = Pattern.compile("[^a-z0-9._-]");

    /**
     * 资源名分隔符
     */
    public static final String NAME_SEPARATOR = "-";

    /**
     * 默认项目名称
     */
    public static final String DEFAULT_PROJECT_NAME = "demo";

    /**
     * 默认环境
     */
    public static final String DEFAULT_ENVIRONMENT = "prod";

    /**
     * 默认流量等级
     */
    public static final String DEFAULT_TRAFFIC_TIER = "standard";

    /**
     * 默认路由域名
     */
    public static final String DEFAULT_HOST = "api.example.com";

    /**
     * 默认路由路径
     */
    public static final String DEFAULT_PATH = "/api";

    /**
     * 默认上游主机
     */
    public static final String DEFAULT_UPSTREAM_HOST = "127.0.0.1";

    /**
     * 默认 HTTP 端口
     */
    public static final int DEFAULT_HTTP_PORT = 8080;

    /**
     * 默认健康检查路径
     */
    public static final String DEFAULT_HEALTH_PATH = "/actuator/health";

    /**
     * 默认健康检查超时
     */
    public static final Duration DEFAULT_HEALTH_TIMEOUT = Duration.ofSeconds(2);

    /**
     * 默认连续失败阈值
     */
    public static final int DEFAULT_UNHEALTHY_THRESHOLD = 2;

    /**
     * 默认限流 QPS
     */
    public static final int DEFAULT_REQUESTS_PER_SECOND = 1000;

    /**
     * 默认限流突发容量
     */
    public static final int DEFAULT_BURST_CAPACITY = 2000;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_MAX_ATTEMPTS = 2;

    /**
     * 默认候选流量权重
     */
    public static final int DEFAULT_CANDIDATE_WEIGHT = 10;

    /**
     * 最大流量权重
     */
    public static final int MAX_TRAFFIC_WEIGHT = 100;

    /**
     * 稳定上游分流目标
     */
    public static final String RELEASE_TARGET_STABLE = "stable";

    /**
     * 候选上游分流目标
     */
    public static final String RELEASE_TARGET_CANDIDATE = "candidate";

    /**
     * 默认染色请求头
     */
    public static final String DEFAULT_COLOR_HEADER = "X-GatePilot-Color";

    /**
     * 默认候选染色值
     */
    public static final String DEFAULT_CANDIDATE_COLOR = "canary";

    /**
     * 创建动作
     */
    public static final String ACTION_CREATE = "CREATE";

    /**
     * 更新动作
     */
    public static final String ACTION_UPDATE = "UPDATE";

    /**
     * 不变动作
     */
    public static final String ACTION_UNCHANGED = "UNCHANGED";

    /**
     * dry-run 错误级别
     */
    public static final String DRY_RUN_LEVEL_ERROR = "ERROR";

    /**
     * 模板校验失败原因
     */
    public static final String REASON_TEMPLATE_INVALID = "TemplateInvalid";

    /**
     * 保存失败提示
     */
    public static final String MESSAGE_TEMPLATE_DRY_RUN_FAILED = "模板校验未通过，不能保存";

    /**
     * 预览完成提示
     */
    public static final String MESSAGE_TEMPLATE_PREVIEW_DONE = "模板预览已生成";

    /**
     * dry-run 完成提示
     */
    public static final String MESSAGE_TEMPLATE_DRY_RUN_DONE = "模板 dry-run 完成";

    /**
     * 保存完成提示
     */
    public static final String MESSAGE_TEMPLATE_APPLY_DONE = "模板资源已保存";

    /**
     * 负载均衡策略不支持提示前缀
     */
    public static final String MESSAGE_UNSUPPORTED_LOAD_BALANCE_PREFIX = "当前负载均衡策略暂不支持: ";

    /**
     * 模板不支持的资源类型提示前缀
     */
    public static final String MESSAGE_UNSUPPORTED_TEMPLATE_RESOURCE_TYPE = "模板不支持资源类型: ";

    /**
     * 项目资源名后缀
     */
    public static final String SUFFIX_PROJECT = "";

    /**
     * 路由资源名后缀
     */
    public static final String SUFFIX_ROUTE = "route";

    /**
     * 稳定上游资源名后缀
     */
    public static final String SUFFIX_STABLE_UPSTREAM = "stable";

    /**
     * 候选上游资源名后缀
     */
    public static final String SUFFIX_CANDIDATE_UPSTREAM = "candidate";

    /**
     * 流量策略资源名后缀
     */
    public static final String SUFFIX_TRAFFIC_POLICY = "traffic";

    /**
     * 认证策略资源名后缀
     */
    public static final String SUFFIX_AUTH_POLICY = "auth";

    /**
     * 发布策略资源名后缀
     */
    public static final String SUFFIX_RELEASE_POLICY = "release";

    /**
     * 模板默认发布说明
     */
    public static final String DEFAULT_RELEASE_DESCRIPTION = "通过项目接入模板生成";

    /**
     * 模板创建人
     */
    public static final String DEFAULT_CREATED_BY = "gatepilot-template";

    /**
     * 默认可重试状态码
     */
    public static final Set<Integer> DEFAULT_RETRY_STATUSES = Set.of(502, 503, 504);

    private ProjectTemplateConstants() {
        // 项目接入模板常量不允许实例化
    }
}
