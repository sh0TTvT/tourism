package com.tourismqa.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record GeocodeRoutePointsRequest(
        @NotBlank(message = "目的地不能为空")
        @Size(max = 120, message = "目的地长度不能超过 120")
        String destination,

        @NotEmpty(message = "至少提供一个行程点位")
        List<@Valid RoutePointSaveRequest> points
) {
}
