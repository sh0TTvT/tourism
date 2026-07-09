package com.tourismqa.dto;

import java.time.Instant;
import java.util.List;

public record AdminUserPublishedRouteResponse(
        Long id,
        String title,
        String content,
        String summary,
        String imageUrl,
        String locationTag,
        String destination,
        int days,
        String interests,
        String budget,
        String departure,
        int clickCount,
        int applyCount,
        Instant createdAt,
        int commentCount,
        List<AdminUserPublishedRoutePointResponse> points,
        List<String> tips,
        List<AdminUserPublishedPostCommentItemResponse> comments
) {
}
