package com.tourismqa.dto;

import com.tourismqa.entity.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * AdminUserUpdateRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record AdminUserUpdateRequest(
        @Size(max = 60, message = "昵称长度不能超过 60")
        String displayName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 120, message = "邮箱长度不能超过 120")
        String email,

        UserRole role,

        Boolean banned,

        @Size(max = 255, message = "封禁原因长度不能超过 255")
        String banReason
) {
}
