package com.tourismqa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 聊天流式事件。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatStreamEvent(
        String type,
        String content,
        Long conversationId,
        String answer,
        Boolean suggestRoute,
        String routeHint,
        String message,
        Boolean usedWeatherContext,
        Boolean usedAttractionStatusContext
) {

    public static ChatStreamEvent meta(Long conversationId) {
        return new ChatStreamEvent("meta", null, conversationId, null, null, null, null, null, null);
    }

    public static ChatStreamEvent delta(String content) {
        return new ChatStreamEvent("delta", content, null, null, null, null, null, null, null);
    }

    public static ChatStreamEvent done(Long conversationId,
                                       String answer,
                                       boolean suggestRoute,
                                       String routeHint,
                                       boolean usedWeatherContext,
                                       boolean usedAttractionStatusContext) {
        return new ChatStreamEvent(
                "done",
                null,
                conversationId,
                answer,
                suggestRoute,
                routeHint,
                null,
                usedWeatherContext,
                usedAttractionStatusContext
        );
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, null, null, null, null, message, null, null);
    }
}
