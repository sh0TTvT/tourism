package com.tourismqa.dto;

import java.util.List;

/**
 * 用户端可见的公共服务状态响应。
 */
public record PublicServiceStatusResponse(
        List<PublicExternalServiceStatusItemResponse> services,
        PublicMapConfigResponse mapConfig
) {
}
