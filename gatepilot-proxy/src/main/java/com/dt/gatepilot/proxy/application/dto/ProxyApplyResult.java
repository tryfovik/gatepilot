package com.dt.gatepilot.proxy.application.dto;

import com.dt.gatepilot.domain.enums.ConfigApplyState;
import java.time.Instant;
import lombok.Data;

/**
 * proxy 本机配置应用结果。
 */
@Data
public class ProxyApplyResult {

    /**
     * 发布版本。
     */
    private String version;

    /**
     * 配置哈希。
     */
    private String configHash;

    /**
     * 应用状态。
     */
    private ConfigApplyState state;

    /**
     * 开始时间。
     */
    private Instant startedAt;

    /**
     * 完成时间。
     */
    private Instant finishedAt;

    /**
     * 失败原因码。
     */
    private String reason;

    /**
     * 失败或结果说明。
     */
    private String message;

    /**
     * 创建成功结果。
     *
     * @param version 发布版本
     * @param configHash 配置哈希
     * @param startedAt 开始时间
     * @return 应用结果
     */
    public static ProxyApplyResult applied(String version, String configHash, Instant startedAt) {
        ProxyApplyResult result = new ProxyApplyResult();
        result.setVersion(version);
        result.setConfigHash(configHash);
        result.setState(ConfigApplyState.APPLIED);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        result.setMessage("配置已应用");
        return result;
    }

    /**
     * 创建失败结果。
     *
     * @param version 发布版本
     * @param configHash 配置哈希
     * @param reason 失败原因码
     * @param message 失败说明
     * @param startedAt 开始时间
     * @return 应用结果
     */
    public static ProxyApplyResult failed(String version,
                                          String configHash,
                                          String reason,
                                          String message,
                                          Instant startedAt) {
        ProxyApplyResult result = new ProxyApplyResult();
        result.setVersion(version);
        result.setConfigHash(configHash);
        result.setState(ConfigApplyState.FAILED);
        result.setStartedAt(startedAt);
        result.setFinishedAt(Instant.now());
        result.setReason(reason);
        result.setMessage(message);
        return result;
    }
}
