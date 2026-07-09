package com.tourismqa.dto;

/**
 * 管理端外部服务保存响应。
 */
public record AdminExternalServiceSaveResponse(
        AdminExternalServiceItemResponse service,
        boolean serviceAvailable,
        String message
) {
}
