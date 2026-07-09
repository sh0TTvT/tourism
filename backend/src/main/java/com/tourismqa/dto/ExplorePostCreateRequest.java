package com.tourismqa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record ExplorePostCreateRequest(
        @Size(max = 120, message = "帖子标题长度不能超过 120")
        String title,

        @Size(max = 4000, message = "帖子内容长度不能超过 4000")
        String content,

        @Size(max = 10, message = "图片最多10张")
        java.util.List<@Size(max = 4000000, message = "图片内容过大") String> imageUrls,

        @Valid
        ExploreSharedRouteRequest route
) {
}
