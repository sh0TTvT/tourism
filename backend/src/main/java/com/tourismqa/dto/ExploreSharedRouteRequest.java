package com.tourismqa.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ExploreSharedRouteRequest(
        @NotBlank(message = "共享路线标题不能为空")
        @Size(max = 180, message = "共享路线标题长度不能超过 180")
        String title,

        @NotBlank(message = "共享路线摘要不能为空")
        @Size(max = 8000, message = "共享路线摘要长度不能超过 8000")
        String summary,

        @NotBlank(message = "共享路线目的地不能为空")
        @Size(max = 120, message = "共享路线目的地长度不能超过 120")
        String destination,

        @Min(value = 1, message = "共享路线天数至少为 1")
        @Max(value = 14, message = "共享路线天数最多为 14")
        Integer days,

        @Size(max = 200, message = "共享路线兴趣偏好长度不能超过 200")
        String interests,

        @Size(max = 120, message = "共享路线预算信息长度不能超过 120")
        String budget,

        @Size(max = 120, message = "共享路线出发地长度不能超过 120")
        String departure,

        List<@Size(max = 120, message = "共享路线提醒长度不能超过 120") String> tips,

        @NotEmpty(message = "共享路线至少需要一个点位")
        List<@Valid RoutePointSaveRequest> points
) {
}
