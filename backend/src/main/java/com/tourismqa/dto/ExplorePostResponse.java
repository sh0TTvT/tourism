package com.tourismqa.dto;

import java.time.Instant;
import java.util.List;

public record ExplorePostResponse(
        Long id,
        String authorName,
        Instant createdAt,
        String title,
        String content,
        java.util.List<String> imageUrls,
        String locationTag,
        boolean own,
        boolean liked,
        boolean favorited,
        int likeCount,
        int favoriteCount,
        int clickCount,
        int applyCount,
        List<ExplorePostCommentResponse> comments,
        ExploreSharedRouteResponse route
) {
}
