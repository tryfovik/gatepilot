package com.dt.gatepilot.apiserver.infrastructure.persistence.mybatisplus;

import com.dt.gatepilot.apiserver.infrastructure.config.GatePilotApiserverConstants;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * GatePilot 资源 Mapper 配置。
 */
@Configuration
@ConditionalOnGatePilotApiserverEnabled
@MapperScan(basePackageClasses = GatePilotResourceMapper.class)
@ConditionalOnProperty(prefix = GatePilotApiserverConstants.STORE_CONFIG_PREFIX,
        name = GatePilotApiserverConstants.STORE_TYPE_PROPERTY,
        havingValue = GatePilotApiserverConstants.STORE_TYPE_DATABASE)
public class GatePilotResourceMapperConfiguration {
}
