package com.dt.gatepilot.apiserver.application.service;

import java.util.UUID;

/**
 * 发布请求标识与版本生成器。
 */
class GatePilotReleaseIdentityGenerator {

    /**
     * 生成发布请求标识。
     *
     * @return 发布请求标识
     */
    String nextReleaseId() {
        return GatePilotReleaseConstants.RELEASE_ID_PREFIX + UUID.randomUUID();
    }

    /**
     * 生成回滚请求标识。
     *
     * @return 回滚请求标识
     */
    String nextRollbackId() {
        return GatePilotReleaseConstants.ROLLBACK_ID_PREFIX + UUID.randomUUID();
    }

    /**
     * 生成发布版本。
     *
     * @param projectName 项目名称
     * @param releaseId 发布请求标识
     * @return 发布版本
     */
    String releaseVersion(String projectName, String releaseId) {
        return version(projectName, GatePilotReleaseConstants.RELEASE_VERSION_PART, releaseId);
    }

    /**
     * 生成回滚版本。
     *
     * @param projectName 项目名称
     * @param releaseId 回滚请求标识
     * @return 回滚版本
     */
    String rollbackVersion(String projectName, String releaseId) {
        return version(projectName, GatePilotReleaseConstants.ROLLBACK_VERSION_PART, releaseId);
    }

    /**
     * 生成 dry-run 版本。
     *
     * @param projectName 项目名称
     * @return dry-run 版本
     */
    String dryRunVersion(String projectName) {
        return version(projectName, GatePilotReleaseConstants.DRY_RUN_VERSION_PART, nextReleaseId());
    }

    private String version(String projectName, String action, String releaseId) {
        // 版本号只表达项目、动作和发布单，不依赖本机时间
        return projectName
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + action
                + GatePilotReleaseConstants.VERSION_SEPARATOR
                + versionSuffix(releaseId);
    }

    private String versionSuffix(String releaseId) {
        String normalized = releaseId.replace(GatePilotReleaseConstants.VERSION_SEPARATOR,
                GatePilotReleaseConstants.EMPTY_VERSION_PART);
        if (normalized.length() <= GatePilotReleaseConstants.VERSION_ID_SUFFIX_LENGTH) {
            return normalized;
        }
        return normalized.substring(normalized.length() - GatePilotReleaseConstants.VERSION_ID_SUFFIX_LENGTH);
    }
}
