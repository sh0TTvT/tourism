package com.tourismqa.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * RoutePlanRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record RoutePlanRequest(
        @NotBlank(message = "目的地不能为空")
        String destination,

        @Min(value = 1, message = "天数至少为1")
        @Max(value = 14, message = "天数最多为14")
        Integer days,

        LocalDate startDate,
        LocalDate endDate,
        String interests,
        String budget,
        String departure,
        Long conversationId,
        String provider,
        String model
) {
}
