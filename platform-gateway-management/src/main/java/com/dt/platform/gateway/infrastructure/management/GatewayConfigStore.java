package com.dt.platform.gateway.infrastructure.management;

import com.dt.platform.gateway.infrastructure.config.GatewayProperties;
import com.dt.platform.gateway.infrastructure.route.GatewayRouteDefinitionLocator;
import com.dt.platform.gateway.infrastructure.validation.GatewayPropertiesValidator;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 管理端保存当前生效网关配置和编译结果。
 */
public class GatewayConfigStore {

    private final AtomicReference<GatewayProperties> activeProperties = new AtomicReference<>();

    private final AtomicReference<GatewayRouteDefinitionLocator> activeRouteDefinitionLocator = new AtomicReference<>();

    /**
     * 创建配置存储。
     *
     * @param initialProperties 初始配置
     */
    public GatewayConfigStore(GatewayProperties initialProperties) {
        activate(initialProperties);
    }

    /**
     * 返回管理端当前生效配置。
     *
     * @return 当前生效配置
     */
    public GatewayProperties activeProperties() {
        return activeProperties.get();
    }

    /**
     * 返回当前生效配置对应的路由编译结果。
     *
     * @return 当前路由定义定位器
     */
    public GatewayRouteDefinitionLocator activeRouteDefinitionLocator() {
        return activeRouteDefinitionLocator.get();
    }

    /**
     * 激活一份新配置。
     *
     * @param properties 待激活配置
     */
    public void activate(GatewayProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("gateway active config must not be null");
        }
        new GatewayPropertiesValidator(properties);
        GatewayRouteDefinitionLocator locator = new GatewayRouteDefinitionLocator(properties);
        activeProperties.set(properties);
        activeRouteDefinitionLocator.set(locator);
    }
}
