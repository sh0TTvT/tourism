package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExplorePostCommentRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论内容长度不能超过 500")
        String content
) {
}
