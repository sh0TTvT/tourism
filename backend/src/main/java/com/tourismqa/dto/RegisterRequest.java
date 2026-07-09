package com.tourismqa.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 40, message = "用户名长度不能超过40")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{2,39}$", message = "用户名需以字母开头，可包含字母、数字和下划线")
        String username,

        @Email(message = "邮箱格式不正确")
        @Size(max = 120, message = "邮箱长度不能超过 120")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码长度至少6位")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "密码仅支持大小写字母和数字")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 30, message = "昵称长度不能超过30")
        String displayName
) {
}
