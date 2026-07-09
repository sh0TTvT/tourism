package com.tourismqa.dto;

import java.time.Instant;
import java.util.List;

public record AdminUserPublishedPostResponse(
        Long id,
        String title,
        String content,
        java.util.List<String> imageUrls,
        String locationTag,
        Instant createdAt,
        int commentCount,
        List<AdminUserPublishedPostCommentItemResponse> comments
) {
}
