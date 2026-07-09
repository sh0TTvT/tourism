package com.tourismqa.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 管理端外部服务保存请求。
 */
public record AdminExternalServiceSaveRequest(
        @NotBlank(message = "服务名称不能为空")
        String displayName,

        @NotNull(message = "enabled 不能为空")
        Boolean enabled,

        @NotBlank(message = "服务地址不能为空")
        String baseUrl,

        Map<String, Object> settings
) {
}
