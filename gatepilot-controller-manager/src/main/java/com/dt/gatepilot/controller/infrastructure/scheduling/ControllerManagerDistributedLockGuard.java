/*
 * Copyright (c) 2026 qiheng. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dt.gatepilot.controller.infrastructure.scheduling;

import com.dt.gatepilot.controller.infrastructure.config.ControllerManagerConstants;
import com.dt.gatepilot.controller.infrastructure.config.GatePilotControllerManagerProperties;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.ClassUtils;

/**
 * controller-manager 分布式锁启动守卫
 */
public class ControllerManagerDistributedLockGuard implements SmartInitializingSingleton {

    private final GatePilotControllerManagerProperties properties;

    private final ListableBeanFactory beanFactory;

    private final ClassLoader classLoader;

    /**
     * 创建分布式锁启动守卫
     *
     * @param properties controller-manager 配置
     * @param beanFactory Bean 工厂
     */
    public ControllerManagerDistributedLockGuard(GatePilotControllerManagerProperties properties,
                                                 ListableBeanFactory beanFactory) {
        this.properties = properties;
        this.beanFactory = beanFactory;
        this.classLoader = ControllerManagerDistributedLockGuard.class.getClassLoader();
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isDistributedLockRequired()) {
            return;
        }
        if (!hasControllerManagerExecutor()) {
            return;
        }
        if (!hasGetbootLockAspect()) {
            throw new IllegalStateException(ControllerManagerConstants.MESSAGE_LOCK_IMPLEMENTATION_MISSING);
        }
    }

    private boolean hasControllerManagerExecutor() {
        // 只有真正创建调度执行器时才需要校验锁实现
        return hasBean(ReleaseReconcileExecutor.class) || hasBean(PublishedConfigStatusRefreshExecutor.class);
    }

    private boolean hasGetbootLockAspect() {
        for (String className : ControllerManagerConstants.GETBOOT_LOCK_ASPECT_CLASSES) {
            if (hasBean(className)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBean(String className) {
        if (!ClassUtils.isPresent(className, classLoader)) {
            return false;
        }
        Class<?> type = ClassUtils.resolveClassName(className, classLoader);
        return hasBean(type);
    }

    private boolean hasBean(Class<?> type) {
        return beanFactory.getBeanNamesForType(type, false, false).length > 0;
    }
}
