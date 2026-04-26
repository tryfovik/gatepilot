package com.dt.gatepilot.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * GatePilot 示例上游启动入口
 */
@SpringBootApplication
public class GatePilotDemoUpstreamApplication {

    /**
     * 启动示例上游服务
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GatePilotDemoUpstreamApplication.class, args);
    }
}
