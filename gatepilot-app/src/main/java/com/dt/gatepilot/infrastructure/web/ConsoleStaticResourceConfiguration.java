package com.dt.gatepilot.infrastructure.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Console 静态资源回退配置，支持 Vue history 路由。
 */
@Configuration
public class ConsoleStaticResourceConfiguration {

    private static final ClassPathResource INDEX_HTML = new ClassPathResource(ConsoleWebConstants.INDEX_HTML_PATH);

    /**
     * Console 前端路由 fallback。
     *
     * @return 路由函数
     */
    @Bean
    public RouterFunction<ServerResponse> consoleIndexRouter() {
        // Vue history 路由统一回落到 index.html
        return RouterFunctions.route(RequestPredicates.GET(ConsoleWebConstants.ROOT_ROUTE), this::index)
                .andRoute(RequestPredicates.GET(ConsoleWebConstants.TOP_LEVEL_FALLBACK_ROUTE), this::index)
                .andRoute(RequestPredicates.GET(ConsoleWebConstants.NESTED_FALLBACK_ROUTE), this::index);
    }

    private reactor.core.publisher.Mono<ServerResponse> index(
            org.springframework.web.reactive.function.server.ServerRequest request) {
        // 静态资源不存在时让前端路由自己接管
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(BodyInserters.fromResource(INDEX_HTML));
    }
}
