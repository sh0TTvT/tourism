package com.tourismqa.dto;

import java.time.Instant;

/**
 * AdminUserItemResponse 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record AdminUserItemResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String role,
        boolean admin,
        boolean banned,
        String banReason,
        Instant createdAt,
        Instant updatedAt
) {
}
