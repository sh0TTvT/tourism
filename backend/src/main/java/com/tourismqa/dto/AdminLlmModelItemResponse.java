package com.tourismqa.dto;

import java.time.Instant;

/**
 * AdminLlmModelItemResponse 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record AdminLlmModelItemResponse(
        Long id,
        String provider,
        String modelId,
        String displayName,
        String baseUrl,
        boolean apiKeyConfigured,
        String apiKeyMasked,
        boolean enabled,
        boolean defaultModel,
        boolean available,
        String unavailableReason,
        Instant lastCheckedAt,
        Boolean lastCheckPassed,
        String lastCheckMessage,
        long totalCallCount,
        long successfulCallCount,
        long failedCallCount,
        Long averageLatencyMs,
        Long lastLatencyMs,
        Instant lastCalledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
