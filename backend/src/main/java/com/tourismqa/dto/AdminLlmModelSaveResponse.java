package com.tourismqa.dto;

/**
 * AdminLlmModelSaveResponse 数据传输对象。
 * 使用场景：用于管理端保存模型配置后返回保存结果与可用性测试结论。
 */
public record AdminLlmModelSaveResponse(
        AdminLlmModelItemResponse model,
        boolean serviceAvailable,
        boolean autoDisabled,
        String message
) {
}
