package com.tourismqa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tourismqa.dto.ModelsResponse;
import com.tourismqa.service.ModelCatalogService;

/**
 * 模型目录查询控制器。
 * 使用场景：
 * 面向前端公开可用模型列表，供聊天界面进行模型选择。
 * 核心职责：
 * 1. 提供只读模型目录 API。
 * 2. 屏蔽底层模型配置存储细节。
 *
 * <p>框架作用：`@RestController` 负责 JSON 序列化输出，默认单例作用域。</p>
 */
@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelCatalogService modelCatalogService;

    public ModelController(ModelCatalogService modelCatalogService) {
        this.modelCatalogService = modelCatalogService;
    }

    /**
     * 查询当前可用模型列表。
     *
     * @return 模型目录响应
     */
    @GetMapping
    public ModelsResponse models() {
        return modelCatalogService.listModels();
    }
}
