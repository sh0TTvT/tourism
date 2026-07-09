package com.tourismqa.dto;

import java.time.Instant;

/**
 * 管理端外部服务测试结果响应。
 */
public record AdminExternalServiceTestResponse(
        String serviceKey,
        boolean available,
        String message,
        Instant testedAt,
        Long latencyMs
) {
}
