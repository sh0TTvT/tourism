package com.tourismqa.dto;

import java.time.Instant;

public record AdminUserPublishedCommentResponse(
        Long id,
        Long postId,
        String postTitle,
        boolean routePost,
        String content,
        Instant createdAt
) {
}
