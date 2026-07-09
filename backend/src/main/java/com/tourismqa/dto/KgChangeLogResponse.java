package com.tourismqa.dto;

import java.time.Instant;

public record KgChangeLogResponse(
        Long id,
        String targetType,
        Long targetId,
        String action,
        String targetLabel,
        Long operatorUserId,
        String operatorUsername,
        String operatorDisplayName,
        Instant createdAt
) {
}
