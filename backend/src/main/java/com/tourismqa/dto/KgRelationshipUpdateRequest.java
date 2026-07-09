package com.tourismqa.dto;

import java.util.Map;

import jakarta.validation.constraints.Size;

/**
 * KgRelationshipUpdateRequest 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record KgRelationshipUpdateRequest(
        @Size(max = 100, message = "关系谓词长度不能超过 100")
        String predicate,

        @Size(max = 1000, message = "关系描述长度不能超过 1000")
        String description,

        Double weight,
        Map<String, Object> attributes
) {
}
