package com.tourismqa.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * RoutePlanResponse 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record RoutePlanResponse(
        Long routePlanId,
        Long conversationId,
        String title,
        String summary,
        String destination,
        Integer days,
        LocalDate startDate,
        LocalDate endDate,
        String interests,
        String budget,
        String departure,
        List<RoutePointDto> points,
        List<String> tips,
        Instant createdAt,
        Instant updatedAt
) {
}
