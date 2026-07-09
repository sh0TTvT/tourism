package com.tourismqa.dto;

/**
 * KgSourceReference 数据传输对象。
 * 使用场景：表示知识图谱检索过程中可供模型或前端展示的来源引用。
 * 核心职责：统一封装来源标题、URL 与来源类型等元信息。
 */
public record KgSourceReference(
        String title,
        String url,
        String source,
        String sourceType,
        String domain
) {
}
