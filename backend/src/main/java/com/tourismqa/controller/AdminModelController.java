package com.tourismqa.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.AdminLlmModelItemResponse;
import com.tourismqa.dto.AdminLlmModelSaveRequest;
import com.tourismqa.dto.AdminLlmModelSaveResponse;
import com.tourismqa.service.ModelCatalogService;

import jakarta.validation.Valid;

/**
 * 管理端模型配置控制器。
 * 使用场景：
 * 为管理员页面提供模型配置的查询、创建、更新、默认切换与删除接口。
 * 核心职责：
 * 1. 承接模型配置管理请求并执行参数校验。
 * 2. 调用模型目录服务完成持久化与业务规则校验。
 * 3. 通过方法级安全控制限制为管理员角色可访问。
 *
 * <p>框架作用：`@RestController` 提供 JSON API；`@PreAuthorize` 在方法调用前执行授权检查。</p>
 */
@RestController
@RequestMapping("/api/admin/models")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModelController {

    private final ModelCatalogService modelCatalogService;

    public AdminModelController(ModelCatalogService modelCatalogService) {
        this.modelCatalogService = modelCatalogService;
    }

    /**
     * 查询管理端可见的全部模型配置。
     *
     * @return 模型配置列表
     */
    @GetMapping
    public List<AdminLlmModelItemResponse> listModels() {
        return modelCatalogService.listModelsForAdmin();
    }

    /**
     * 新增模型配置记录。
     *
     * @param request 模型保存请求
     * @return 新建后的模型配置
     */
    @PostMapping
    public AdminLlmModelSaveResponse createModel(@Valid @RequestBody AdminLlmModelSaveRequest request) {
        return modelCatalogService.createModel(request);
    }

    /**
     * 更新指定模型配置。
     *
     * @param id 模型配置主键
     * @param request 模型保存请求
     * @return 更新后的模型配置
     */
    @PutMapping("/{id}")
    public AdminLlmModelSaveResponse updateModel(@PathVariable Long id,
                                                 @Valid @RequestBody AdminLlmModelSaveRequest request) {
        return modelCatalogService.updateModel(id, request);
    }

    /**
     * 将指定模型设置为对应提供方的默认模型。
     *
     * @param id 模型配置主键
     * @return 默认状态更新后的模型配置
     */
    @PutMapping("/{id}/default")
    public AdminLlmModelItemResponse setDefaultModel(@PathVariable Long id) {
        return modelCatalogService.setDefaultModel(id);
    }

    /**
     * 删除指定模型配置。
     *
     * @param id 模型配置主键
     */
    @DeleteMapping("/{id}")
    public void deleteModel(@PathVariable Long id) {
        modelCatalogService.deleteModel(id);
    }
}
