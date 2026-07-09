package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * ChatMessageDto 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record ChatMessageDto(
        @NotBlank(message = "角色不能为空")
        String role,

        @NotBlank(message = "内容不能为空")
        String content
) {
}
