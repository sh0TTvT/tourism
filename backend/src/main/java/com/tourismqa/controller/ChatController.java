package com.tourismqa.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.tourismqa.dto.ChatConversationDetailResponse;
import com.tourismqa.dto.ChatConversationListResponse;
import com.tourismqa.dto.ChatRequest;
import com.tourismqa.dto.ChatResponse;
import com.tourismqa.dto.SavePartialRequest;
import com.tourismqa.security.UserPrincipal;
import com.tourismqa.service.TourismChatService;

import jakarta.validation.Valid;

/**
 * 对话问答接口控制器。
 * 使用场景：
 * 面向前端聊天页面提供会话创建、历史会话查询与会话消息详情查询能力。
 * 核心职责：
 * 1. 接收并校验 HTTP 请求参数。
 * 2. 读取当前认证用户上下文并传递至业务层。
 * 3. 将业务层结果映射为统一 JSON 响应。
 * 设计说明：
 * 控制器保持轻量，仅负责协议层编排，业务决策统一下沉到 {@link TourismChatService}。
 *
 * <p>框架作用：`@RestController` 使返回值自动序列化为 JSON；默认由 Spring 以单例 Bean 管理。</p>
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final TourismChatService tourismChatService;

    public ChatController(TourismChatService tourismChatService) {
        this.tourismChatService = tourismChatService;
    }

    /**
     * 查询当前用户的历史会话列表，按更新时间倒序返回。
     *
     * @param principal 当前登录用户信息
     * @return 会话列表响应
     */
    @GetMapping("/conversations")
    public ChatConversationListResponse conversations(@AuthenticationPrincipal UserPrincipal principal) {
        return tourismChatService.listConversations(principal);
    }

    /**
     * 查询指定会话的消息明细。
     *
     * @param conversationId 会话主键
     * @param principal 当前登录用户信息
     * @return 会话详情及消息列表
     */
    @GetMapping("/conversations/{conversationId}")
    public ChatConversationDetailResponse conversationDetail(@PathVariable Long conversationId,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return tourismChatService.getConversationDetail(conversationId, principal);
    }

    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable Long conversationId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        tourismChatService.deleteConversation(conversationId, principal);
    }

    /**
     * 提交一轮问答请求并返回模型回复。
     *
     * @param request 问答请求体，包含用户输入与可选历史上下文
     * @param principal 当前登录用户信息
     * @return 模型回复及路由提示信息
     */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request,
                             @AuthenticationPrincipal UserPrincipal principal) {
        return tourismChatService.chat(request, principal);
    }

    @PostMapping(value = "/stream", produces = "application/x-ndjson", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StreamingResponseBody streamChat(@Valid @RequestBody ChatRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return tourismChatService.streamChat(request, principal);
    }

    /**
     * 保存部分对话（用于流式传输中断后的恢复）。
     * 前端在恢复失败后会调用此端点，确保未完成的对话不会丢失。
     *
     * @param conversationId 会话主键
     * @param request 保存部分对话请求体
     * @param principal 当前登录用户信息
     */
    @PostMapping("/conversations/{conversationId}/save-partial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void savePartialExchange(@PathVariable Long conversationId,
                                   @Valid @RequestBody SavePartialRequest request,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        tourismChatService.savePartialExchange(
                conversationId,
                request.userMessage(),
                request.partialAnswer(),
                principal
        );
    }
}
