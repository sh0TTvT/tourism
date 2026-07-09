package com.tourismqa.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.tourismqa.dto.KgContextResponse;
import com.tourismqa.dto.UserLocationDto;

/**
 * 旅游实时上下文聚合服务。
 * 使用场景：
 * 在问答前统一收集天气和景点开放状态等动态信息。
 * 核心职责：
 * 1. 聚合多个动态数据子服务。
 * 2. 生成单段可注入大模型的实时上下文。
 * 3. 保持外部服务失败时主流程可用。
 */
@Service
public class RealtimeTravelContextService {

    private final WeatherContextService weatherContextService;
    private final AttractionStatusService attractionStatusService;

    public RealtimeTravelContextService(WeatherContextService weatherContextService,
                                        AttractionStatusService attractionStatusService) {
        this.weatherContextService = weatherContextService;
        this.attractionStatusService = attractionStatusService;
    }

    public RealtimeContextPayload buildPromptContext(String question, KgContextResponse graphContext) {
        return buildPromptContext(question, graphContext, null);
    }

    public RealtimeContextPayload buildPromptContext(String question,
                                                     KgContextResponse graphContext,
                                                     UserLocationDto userLocation) {
        List<String> parts = new ArrayList<>();
        String weatherContext = weatherContextService.buildPromptContext(question, graphContext, userLocation);
        String attractionStatusContext = attractionStatusService.buildPromptContext(question, graphContext);
        addIfPresent(parts, weatherContext);
        addIfPresent(parts, attractionStatusContext);
        return new RealtimeContextPayload(
                parts.isEmpty() ? null : String.join("\n\n", parts),
                StringUtils.hasText(weatherContext),
                StringUtils.hasText(attractionStatusContext)
        );
    }

    public DirectRealtimeAnswer tryBuildDirectAnswer(String question, KgContextResponse graphContext) {
        return tryBuildDirectAnswer(question, graphContext, null);
    }

    public DirectRealtimeAnswer tryBuildDirectAnswer(String question,
                                                     KgContextResponse graphContext,
                                                     UserLocationDto userLocation) {
        WeatherContextService.DirectWeatherAnswer weatherAnswer =
                weatherContextService.tryBuildDirectAnswer(question, graphContext, userLocation);
        if (weatherAnswer == null) {
            return null;
        }
        return new DirectRealtimeAnswer(weatherAnswer.answer(), true, false);
    }

    private void addIfPresent(List<String> parts, String text) {
        if (StringUtils.hasText(text)) {
            parts.add(text.trim());
        }
    }

    public record DirectRealtimeAnswer(
            String answer,
            boolean usedWeatherContext,
            boolean usedAttractionStatusContext
    ) {
    }
}
