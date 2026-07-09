package com.tourismqa.dto;

import java.time.Instant;

public record ExplorePostCommentResponse(
        Long id,
        String authorName,
        Instant createdAt,
        String content,
        boolean own,
        boolean liked,
        int likeCount
) {
}
