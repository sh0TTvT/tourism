package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * AdminLlmModelSaveRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record AdminLlmModelSaveRequest(
        @NotBlank(message = "provider 不能为空")
        String provider,

        @NotBlank(message = "模型ID不能为空")
        String modelId,

        @NotBlank(message = "显示名称不能为空")
        String displayName,

        @NotBlank(message = "Base URL 不能为空")
        String baseUrl,

        String apiKey,

        @NotNull(message = "enabled 不能为空")
        Boolean enabled,

        Boolean defaultModel
) {
}
