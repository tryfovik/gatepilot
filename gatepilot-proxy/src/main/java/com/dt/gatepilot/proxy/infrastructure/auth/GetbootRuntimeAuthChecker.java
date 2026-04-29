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
package com.dt.gatepilot.proxy.infrastructure.auth;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthChecker;
import com.dt.gatepilot.proxy.domain.port.RuntimeAuthResult;
import com.dt.gatepilot.proxy.domain.runtime.ProxyAuthConstants;
import com.getboot.auth.spi.SaTokenWebFluxAuthChecker;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 基于 getboot-auth 的 proxy 运行时认证校验器。
 */
public class GetbootRuntimeAuthChecker implements RuntimeAuthChecker {

    /**
     * getboot-auth WebFlux 认证校验器提供器。
     */
    private final ObjectProvider<SaTokenWebFluxAuthChecker> checkerProvider;

    /**
     * 创建认证校验器。
     *
     * @param checkerProvider getboot-auth 认证校验器提供器
     */
    public GetbootRuntimeAuthChecker(ObjectProvider<SaTokenWebFluxAuthChecker> checkerProvider) {
        this.checkerProvider = checkerProvider;
    }

    /**
     * 执行当前请求认证校验。
     *
     * @return 认证结果
     */
    @Override
    public RuntimeAuthResult check() {
        SaTokenWebFluxAuthChecker checker = checkerProvider.getIfAvailable();
        if (checker == null) {
            // 策略要求认证但组件缺失时必须失败关闭
            return RuntimeAuthResult.denied(
                    ProxyAuthConstants.UNAVAILABLE_STATUS,
                    ProxyAuthConstants.UNAVAILABLE_CODE,
                    ProxyAuthConstants.UNAVAILABLE_MESSAGE,
                    ProxyAuthConstants.REASON_AUTH_CHECKER_UNAVAILABLE,
                    null
            );
        }
        try {
            checker.check();
            return RuntimeAuthResult.pass();
        } catch (NotLoginException exception) {
            return RuntimeAuthResult.denied(
                    ProxyAuthConstants.UNAUTHORIZED_STATUS,
                    ProxyAuthConstants.UNAUTHORIZED_CODE,
                    ProxyAuthConstants.UNAUTHORIZED_MESSAGE,
                    ProxyAuthConstants.REASON_UNAUTHORIZED,
                    exception
            );
        } catch (NotPermissionException exception) {
            return RuntimeAuthResult.denied(
                    ProxyAuthConstants.FORBIDDEN_STATUS,
                    ProxyAuthConstants.FORBIDDEN_CODE,
                    ProxyAuthConstants.FORBIDDEN_MESSAGE,
                    ProxyAuthConstants.REASON_FORBIDDEN,
                    exception
            );
        } catch (RuntimeException exception) {
            return RuntimeAuthResult.denied(
                    ProxyAuthConstants.UNAUTHORIZED_STATUS,
                    ProxyAuthConstants.UNAUTHORIZED_CODE,
                    ProxyAuthConstants.UNAUTHORIZED_MESSAGE,
                    ProxyAuthConstants.REASON_UNAUTHORIZED,
                    exception
            );
        }
    }
}
