package com.dt.gatepilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GatePilot 单体合包启动入口。
 */
@SpringBootApplication
public class GatePilotApplication {

    /**
     * 启动 GatePilot 单体应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatePilotApplication.class, args);
    }
}
