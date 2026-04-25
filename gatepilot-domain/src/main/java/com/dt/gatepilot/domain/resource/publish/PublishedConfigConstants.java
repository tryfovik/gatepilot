package com.dt.gatepilot.domain.resource.publish;

/**
 * PublishedConfig 配置载荷常量。
 */
public final class PublishedConfigConstants {

    /**
     * 流量策略类型。
     */
    public static final String POLICY_TYPE_TRAFFIC = "TrafficPolicy";

    /**
     * 发布策略类型。
     */
    public static final String POLICY_TYPE_RELEASE = "ReleasePolicy";

    /**
     * 认证策略类型。
     */
    public static final String POLICY_TYPE_AUTH = "AuthPolicy";

    /**
     * targetRefs 配置键。
     */
    public static final String KEY_TARGET_REFS = "targetRefs";

    /**
     * targetSelector 配置键。
     */
    public static final String KEY_TARGET_SELECTOR = "targetSelector";

    /**
     * timeout 配置键。
     */
    public static final String KEY_TIMEOUT = "timeout";

    /**
     * retry 配置键。
     */
    public static final String KEY_RETRY = "retry";

    /**
     * circuitBreaker 配置键。
     */
    public static final String KEY_CIRCUIT_BREAKER = "circuitBreaker";

    /**
     * rateLimit 配置键。
     */
    public static final String KEY_RATE_LIMIT = "rateLimit";

    /**
     * colorRules 配置键。
     */
    public static final String KEY_COLOR_RULES = "colorRules";

    /**
     * strategy 配置键。
     */
    public static final String KEY_STRATEGY = "strategy";

    /**
     * stableUpstreamRef 配置键。
     */
    public static final String KEY_STABLE_UPSTREAM_REF = "stableUpstreamRef";

    /**
     * candidateUpstreamRef 配置键。
     */
    public static final String KEY_CANDIDATE_UPSTREAM_REF = "candidateUpstreamRef";

    /**
     * trafficSplits 配置键。
     */
    public static final String KEY_TRAFFIC_SPLITS = "trafficSplits";

    /**
     * steps 配置键。
     */
    public static final String KEY_STEPS = "steps";

    /**
     * autoRollback 配置键。
     */
    public static final String KEY_AUTO_ROLLBACK = "autoRollback";

    /**
     * type 配置键。
     */
    public static final String KEY_TYPE = "type";

    /**
     * anonymousAllowed 配置键。
     */
    public static final String KEY_ANONYMOUS_ALLOWED = "anonymousAllowed";

    /**
     * credentialRefs 配置键。
     */
    public static final String KEY_CREDENTIAL_REFS = "credentialRefs";

    /**
     * jwt 配置键。
     */
    public static final String KEY_JWT = "jwt";

    /**
     * apiKey 配置键。
     */
    public static final String KEY_API_KEY = "apiKey";

    /**
     * publicPaths 配置键。
     */
    public static final String KEY_PUBLIC_PATHS = "publicPaths";

    /**
     * source 配置键。
     */
    public static final String KEY_SOURCE = "source";

    /**
     * key 配置键。
     */
    public static final String KEY_KEY = "key";

    /**
     * fieldName 配置键。
     */
    public static final String KEY_FIELD_NAME = "fieldName";

    /**
     * name 配置键。
     */
    public static final String KEY_NAME = "name";

    /**
     * match 配置键。
     */
    public static final String KEY_MATCH = "match";

    /**
     * pattern 配置键。
     */
    public static final String KEY_PATTERN = "pattern";

    /**
     * value 配置键。
     */
    public static final String KEY_VALUE = "value";

    /**
     * matchStrategy 配置键。
     */
    public static final String KEY_MATCH_STRATEGY = "matchStrategy";

    /**
     * color 配置键。
     */
    public static final String KEY_COLOR = "color";

    /**
     * target 配置键。
     */
    public static final String KEY_TARGET = "target";

    /**
     * weight 配置键。
     */
    public static final String KEY_WEIGHT = "weight";

    /**
     * weightHashHeaders 配置键。
     */
    public static final String KEY_WEIGHT_HASH_HEADERS = "weightHashHeaders";

    /**
     * trustRequestHeader 配置键。
     */
    public static final String KEY_TRUST_REQUEST_HEADER = "trustRequestHeader";

    /**
     * headerName 配置键。
     */
    public static final String KEY_HEADER_NAME = "headerName";

    /**
     * defaultColor 配置键。
     */
    public static final String KEY_DEFAULT_COLOR = "defaultColor";

    /**
     * enabled 配置键。
     */
    public static final String KEY_ENABLED = "enabled";

    /**
     * slidingWindowSize 配置键。
     */
    public static final String KEY_SLIDING_WINDOW_SIZE = "slidingWindowSize";

    /**
     * minimumNumberOfCalls 配置键。
     */
    public static final String KEY_MINIMUM_NUMBER_OF_CALLS = "minimumNumberOfCalls";

    /**
     * failureRateThreshold 配置键。
     */
    public static final String KEY_FAILURE_RATE_THRESHOLD = "failureRateThreshold";

    /**
     * slowCallRateThreshold 配置键。
     */
    public static final String KEY_SLOW_CALL_RATE_THRESHOLD = "slowCallRateThreshold";

    /**
     * slowCallDurationThreshold 配置键。
     */
    public static final String KEY_SLOW_CALL_DURATION_THRESHOLD = "slowCallDurationThreshold";

    /**
     * waitDurationInOpenState 配置键。
     */
    public static final String KEY_WAIT_DURATION_IN_OPEN_STATE = "waitDurationInOpenState";

    /**
     * permittedNumberOfCallsInHalfOpenState 配置键。
     */
    public static final String KEY_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE =
            "permittedNumberOfCallsInHalfOpenState";

    /**
     * statusCodes 配置键。
     */
    public static final String KEY_STATUS_CODES = "statusCodes";

    /**
     * fallbackStatus 配置键。
     */
    public static final String KEY_FALLBACK_STATUS = "fallbackStatus";

    /**
     * fallbackCode 配置键。
     */
    public static final String KEY_FALLBACK_CODE = "fallbackCode";

    /**
     * fallbackMessage 配置键。
     */
    public static final String KEY_FALLBACK_MESSAGE = "fallbackMessage";

    /**
     * contentType 配置键。
     */
    public static final String KEY_CONTENT_TYPE = "contentType";

    /**
     * requestsPerSecond 配置键。
     */
    public static final String KEY_REQUESTS_PER_SECOND = "requestsPerSecond";

    /**
     * burstCapacity 配置键。
     */
    public static final String KEY_BURST_CAPACITY = "burstCapacity";

    /**
     * paramRules 配置键。
     */
    public static final String KEY_PARAM_RULES = "paramRules";

    /**
     * 隐藏工具类构造器。
     */
    private PublishedConfigConstants() {
        // 发布配置常量不允许实例化
    }
}
