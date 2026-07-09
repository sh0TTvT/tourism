package com.tourismqa.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * ChatRequest 数据传输对象。
 */
public record ChatRequest(
        @NotBlank(message = "消息不能为空")
        String message,

        @Valid
        List<ChatMessageDto> history,

        Long conversationId,

        String provider,
        String model,

        @Valid
        UserLocationDto location
) {
}
