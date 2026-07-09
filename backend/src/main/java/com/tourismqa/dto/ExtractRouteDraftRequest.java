package com.tourismqa.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ExtractRouteDraftRequest(
        @NotEmpty(message = "消息列表不能为空")
        List<@Valid ChatMessageDto> messages
) {
}
