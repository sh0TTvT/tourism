package com.tourismqa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UpdateUserPreferencesRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record UpdateUserPreferencesRequest(
        @Size(max = 120, message = "常用出发地长度不能超过 120")
        String preferredDeparture,

        @Size(max = 80, message = "预算偏好长度不能超过 80")
        String budgetPreference,

        @Size(max = 255, message = "出行偏好长度不能超过 255")
        String travelPreferences,

        @Size(max = 255, message = "兴趣标签长度不能超过 255")
        String interestTags,

        @NotBlank(message = "记忆策略不能为空")
        @Size(max = 40, message = "记忆策略长度不能超过 40")
        String memoryStrategy
) {
}
