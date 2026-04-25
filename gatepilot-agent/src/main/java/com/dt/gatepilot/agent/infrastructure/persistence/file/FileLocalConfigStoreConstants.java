package com.dt.gatepilot.agent.infrastructure.persistence.file;

/**
 * 文件本地配置存储常量。
 */
public final class FileLocalConfigStoreConstants {

    /**
     * staged 配置文件名。
     */
    public static final String STAGED_CONFIG_FILE_NAME = "staged-config.json";

    /**
     * last-good 配置文件名。
     */
    public static final String LAST_GOOD_CONFIG_FILE_NAME = "last-good-config.json";

    /**
     * 临时文件后缀。
     */
    public static final String TEMP_FILE_SUFFIX = ".tmp";

    /**
     * 本地配置读取失败提示。
     */
    public static final String MESSAGE_READ_CONFIG_FAILED = "读取 agent 本地配置失败";

    /**
     * 本地配置写入失败提示。
     */
    public static final String MESSAGE_WRITE_CONFIG_FAILED = "写入 agent 本地配置失败";

    private FileLocalConfigStoreConstants() {
        // 文件本地配置存储常量不允许实例化
    }
}
