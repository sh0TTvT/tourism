package com.tourismqa.dto;

import java.time.Instant;

/**
 * 用户端可见的外部服务状态项。
 */
public record PublicExternalServiceStatusItemResponse(
        String serviceKey,
        String displayName,
        boolean enabled,
        boolean available,
        String message,
        Instant lastHeartbeatAt
) {
}
