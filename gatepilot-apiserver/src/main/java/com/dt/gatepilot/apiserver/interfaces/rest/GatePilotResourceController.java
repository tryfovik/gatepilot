package com.dt.gatepilot.apiserver.interfaces.rest;

import com.dt.gatepilot.apiserver.domain.model.CursorPage;
import com.dt.gatepilot.apiserver.application.service.GatePilotResourceService;
import com.dt.gatepilot.apiserver.infrastructure.config.ConditionalOnGatePilotApiserverEnabled;
import com.fasterxml.jackson.databind.JsonNode;
import com.getboot.web.api.response.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * GatePilot 声明式资源 API。
 */
@RestController
@ConditionalOnGatePilotApiserverEnabled
@RequestMapping(GatePilotApiPaths.RESOURCES)
public class GatePilotResourceController {

    private final GatePilotResourceService resourceService;

    /**
     * 创建资源控制器。
     *
     * @param resourceService 资源服务
     */
    public GatePilotResourceController(GatePilotResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * 查询资源列表。
     *
     * @param resourceType 资源类型
     * @param namespace 命名空间
     * @param cursor 游标
     * @param limit 返回条数
     * @return 资源列表
     */
    @GetMapping("/{resourceType}")
    public Mono<ApiResponse<CursorPage<Object>>> list(@PathVariable String resourceType,
                                                              @RequestParam(required = false) String namespace,
                                                              @RequestParam(required = false) String cursor,
                                                              @RequestParam(required = false) Integer limit) {
        // 列表必须走分页，避免大资源一次性打满接口
        return Mono.just(ApiResponse.success(resourceService.list(resourceType, namespace, cursor, limit)));
    }

    /**
     * 查询单个资源。
     *
     * @param resourceType 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @return 资源对象
     */
    @GetMapping("/{resourceType}/{namespace}/{name}")
    public Mono<ApiResponse<Object>> get(@PathVariable String resourceType,
                                         @PathVariable String namespace,
                                         @PathVariable String name) {
        // 单资源查询只从 apiserver 存储读取
        return Mono.just(ApiResponse.success(resourceService.get(resourceType, namespace, name)));
    }

    /**
     * 保存资源。
     *
     * @param resourceType 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @param body 请求体
     * @return 保存后的资源
     */
    @PutMapping("/{resourceType}/{namespace}/{name}")
    public Mono<ApiResponse<Object>> save(@PathVariable String resourceType,
                                          @PathVariable String namespace,
                                          @PathVariable String name,
                                          @RequestBody JsonNode body) {
        // 保存走资源服务统一做类型解析和 generation 处理
        return Mono.just(ApiResponse.success(resourceService.save(resourceType, namespace, name, body), "保存成功"));
    }

    /**
     * 删除资源。
     *
     * @param resourceType 资源类型
     * @param namespace 命名空间
     * @param name 资源名称
     * @return 删除结果
     */
    @DeleteMapping("/{resourceType}/{namespace}/{name}")
    public Mono<ApiResponse<Void>> delete(@PathVariable String resourceType,
                                          @PathVariable String namespace,
                                          @PathVariable String name) {
        // 删除动作统一收敛在资源服务
        resourceService.delete(resourceType, namespace, name);
        return Mono.just(ApiResponse.success(null, "删除成功"));
    }
}
