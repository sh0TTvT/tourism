package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 保存部分对话请求。
 * 用于在流式传输中断后，前端主动保存未完成的对话。
 */
public record SavePartialRequest(
        @NotBlank(message = "用户消息不能为空")
        String userMessage,

        @NotBlank(message = "部分回答不能为空")
        String partialAnswer
) {
}
