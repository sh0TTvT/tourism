package com.tourismqa.dto;

import java.util.List;

public record AdminUserPublishedContentResponse(
        List<AdminUserPublishedPostResponse> posts,
        List<AdminUserPublishedRouteResponse> routes,
        List<AdminUserPublishedCommentResponse> comments
) {
}
