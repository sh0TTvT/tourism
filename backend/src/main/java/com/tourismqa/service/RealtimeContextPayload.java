package com.tourismqa.service;

/**
 * 实时上下文聚合结果。
 * 使用场景：
 * 在聊天主链路中同时传递实时上下文文本和其来源标记。
 * 核心职责：
 * 1. 封装注入大模型的上下文文本。
 * 2. 标记本轮是否使用了天气与景点开放状态增强。
 */
public record RealtimeContextPayload(
        String promptContext,
        boolean usedWeatherContext,
        boolean usedAttractionStatusContext
) {
}
