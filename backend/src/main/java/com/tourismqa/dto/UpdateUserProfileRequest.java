package com.tourismqa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UpdateUserProfileRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record UpdateUserProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 60, message = "昵称长度不能超过 60")
        String displayName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 120, message = "邮箱长度不能超过 120")
        String email
) {
}
