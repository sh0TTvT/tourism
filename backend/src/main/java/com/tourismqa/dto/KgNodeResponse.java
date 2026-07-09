package com.tourismqa.dto;

import java.util.List;
import java.util.Map;

/**
 * KgNodeResponse 数据传输对象。
 * 使用场景：用于接口层与业务层之间的数据交换与序列化传输。
 * 核心职责：定义稳定字段契约，并通过字段校验注解约束输入边界。
 */
public record KgNodeResponse(
        Long id,
        String name,
        String category,
        String description,
        List<String> aliases,
        List<String> tags,
        Map<String, Object> attributes,
        String createdAt,
        String updatedAt,
        Long createdByUserId,
        String createdByUsername,
        String createdByDisplayName,
        Long updatedByUserId,
        String updatedByUsername,
        String updatedByDisplayName
) {
    public KgNodeResponse(Long id,
                          String name,
                          String category,
                          String description,
                          List<String> aliases,
                          List<String> tags,
                          Map<String, Object> attributes,
                          String createdAt,
                          String updatedAt) {
        this(id, name, category, description, aliases, tags, attributes, createdAt, updatedAt,
                null, null, null, null, null, null);
    }
}
