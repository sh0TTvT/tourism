package com.tourismqa.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateRoutePlanRequest(
        @NotBlank(message = "路线标题不能为空")
        @Size(max = 180, message = "路线标题长度不能超过 180")
        String title,

        @NotBlank(message = "路线概述不能为空")
        @Size(max = 8000, message = "路线概述长度不能超过 8000")
        String summary,

        @NotBlank(message = "目的地不能为空")
        @Size(max = 120, message = "目的地长度不能超过 120")
        String destination,

        @Min(value = 1, message = "天数至少为 1")
        @Max(value = 14, message = "天数最多为 14")
        Integer days,

        LocalDate startDate,
        LocalDate endDate,

        @Size(max = 200, message = "兴趣偏好长度不能超过 200")
        String interests,

        @Size(max = 120, message = "预算信息长度不能超过 120")
        String budget,

        @Size(max = 120, message = "出发地长度不能超过 120")
        String departure,

        List<@Size(max = 120, message = "提醒项长度不能超过 120") String> tips,

        @NotEmpty(message = "至少保留一个行程点位")
        List<@Valid RoutePointSaveRequest> points,

        Long conversationId
) {
}
