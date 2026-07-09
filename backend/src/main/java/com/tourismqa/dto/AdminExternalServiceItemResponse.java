package com.tourismqa.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 管理端外部服务配置项响应。
 */
public record AdminExternalServiceItemResponse(
        Long id,
        String serviceKey,
        String displayName,
        String description,
        boolean enabled,
        String baseUrl,
        Map<String, Object> settings,
        boolean available,
        String statusMessage,
        Instant lastCheckedAt,
        Boolean lastCheckPassed,
        String lastCheckMessage,
        Instant lastHeartbeatAt,
        Boolean lastHeartbeatPassed,
        String lastHeartbeatMessage,
        Long lastHeartbeatLatencyMs,
        Instant createdAt,
        Instant updatedAt
) {
}
