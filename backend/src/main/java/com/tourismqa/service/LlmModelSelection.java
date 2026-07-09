package com.tourismqa.service;

/**
 * 模型路由结果对象。
 * 使用场景：
 * 由模型目录服务返回最终可调用的提供方与模型标识。
 *
 * @param provider 模型提供方标识
 * @param modelId 模型 ID
 */
public record LlmModelSelection(
        String provider,
        String modelId
) {
}
