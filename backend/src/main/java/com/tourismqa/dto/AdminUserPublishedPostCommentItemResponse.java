package com.tourismqa.dto;

import java.time.Instant;

public record AdminUserPublishedPostCommentItemResponse(
        Long id,
        String authorName,
        boolean own,
        String content,
        Instant createdAt
) {
}
