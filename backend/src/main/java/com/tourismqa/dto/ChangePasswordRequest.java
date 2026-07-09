package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ChangePasswordRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        String currentPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 120, message = "新密码长度应为 6-120 位")
        String newPassword
) {
}
