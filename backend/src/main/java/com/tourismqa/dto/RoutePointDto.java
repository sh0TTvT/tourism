package com.tourismqa.dto;

/**
 * RoutePointDto 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record RoutePointDto(
        int day,
        int order,
        String name,
        String description,
        Double latitude,
        Double longitude
) {
}
