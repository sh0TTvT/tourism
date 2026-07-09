package com.tourismqa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * RoutePointSaveRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record RoutePointSaveRequest(
        @Min(value = 1, message = "行程天数序号至少为 1")
        Integer day,

        @Min(value = 1, message = "点位顺序至少为 1")
        Integer order,

        @NotBlank(message = "点位名称不能为空")
        @Size(max = 150, message = "点位名称长度不能超过 150")
        String name,

        @Size(max = 2000, message = "点位描述长度不能超过 2000")
        String description,

        Double latitude,
        Double longitude
) {
}
