package com.dt.gatepilot.apiserver.infrastructure.persistence.jdbc;

import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot 资源 Mapper 配置。
 */
@Configuration
@MapperScan(basePackageClasses = GatePilotResourceMapper.class)
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_JDBC)
public class GatePilotResourceMapperConfiguration {
}
