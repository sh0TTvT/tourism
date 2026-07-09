import { nextTick, reactive } from "vue";
import { requestJson, requestNdjsonStream } from "../../api/request";

export function createChatActions(ctx) {
  let activeChatAbortController = null;

  function resetChatInput() {
    ctx.chatInput.value = "";
  }

  function createChatMessage(message = {}) {
    return reactive({
      role: message.role || "assistant",
      content: message.content || "",
      createdAt: message.createdAt || new Date().toISOString(),
      suggestRoute: Boolean(message.suggestRoute),
      routeHint: message.routeHint || "",
      usedWeatherContext: Boolean(message.usedWeatherContext),
      usedAttractionStatusContext: Boolean(message.usedAttractionStatusContext),
      sourceInfo: message.sourceInfo || null,
      sources: Array.isArray(message.sources) ? message.sources : [],
      references: Array.isArray(message.references) ? message.references : [],
      streaming: Boolean(message.streaming),
      recovering: Boolean(message.recovering),
    });
  }

  function mapConversationMessages(detail) {
    return Array.isArray(detail?.messages)
      ? detail.messages.map((message) =>
          createChatMessage({
            role: message.role,
            content: message.content,
            createdAt: message.createdAt,
            sourceInfo: message.sourceInfo,
            sources: message.sources,
            references: message.references,
          }),
        )
      : [];
  }

  function sleep(ms) {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
  }

  async function recoverInterruptedChatStream(conversationId, persistedCountBeforeSend) {
    if (!conversationId) return false;
    const deadline = Date.now() + 45000;

    while (Date.now() < deadline) {
      await sleep(1500);

      try {
        const detail = await requestJson(`/api/chat/conversations/${conversationId}`);
        const recoveredMessages = mapConversationMessages(detail);
        const lastMessage = recoveredMessages[recoveredMessages.length - 1];

        const hasCompletedExchange =
          recoveredMessages.length >= persistedCountBeforeSend + 2 &&
          lastMessage?.role === "assistant" &&
          Boolean(lastMessage?.content);

        if (!hasCompletedExchange) continue;

        if (ctx.selectedConversationId.value === conversationId) {
          ctx.messages.value = recoveredMessages;
          await nextTick();
          ctx.chatWorkspaceRef.value?.scrollToBottom?.();
        }

        await loadConversations();
        return true;
      } catch {}
    }

    return false;
  }

  function startNewChat() {
    ctx.selectedConversationId.value = null;
    ctx.messages.value = [];
    ctx.switchView("chat");
    ctx.closeMobileSidebar();
    resetChatInput();
  }

  function useStarterPrompt(prompt) {
    ctx.switchView("chat");
    ctx.chatInput.value = prompt;

    nextTick(() => {
      ctx.chatWorkspaceRef.value?.focusComposer?.();
    });
  }

  async function startFreshChatWithPrompt(prompt, options = {}) {
    ctx.selectedConversationId.value = null;
    ctx.messages.value = [];
    ctx.switchView("chat");
    ctx.closeMobileSidebar();
    ctx.chatInput.value = prompt;

    await nextTick();

    if (options.autoSend) {
      await sendMessage();
      return;
    }

    ctx.chatWorkspaceRef.value?.focusComposer?.();
  }

  async function loadConversations() {
    if (!ctx.isAuthenticated.value) {
      ctx.conversations.value = [];
      return;
    }

    ctx.loadingConversations.value = true;

    try {
      const data = await requestJson("/api/chat/conversations");
      ctx.conversations.value = Array.isArray(data?.conversations) ? data.conversations : [];
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.loadingConversations.value = false;
    }
  }

  async function selectConversation(item) {
    if (!item?.id) return;

    ctx.switchView("chat");
    ctx.selectedConversationId.value = item.id;

    try {
      const detail = await requestJson(`/api/chat/conversations/${item.id}`);
      ctx.messages.value = mapConversationMessages(detail);
      await nextTick();
      ctx.chatWorkspaceRef.value?.scrollToBottom?.();
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.closeMobileSidebar();
    }
  }

  async function deleteConversation(item) {
    if (!item?.id || ctx.deletingConversationId.value) return;

    const title = item.title || "未命名对话";
    const confirmed = await ctx.requestConfirm({
      title: `删除"${title}"`,
      message: "删除后该聊天记录、消息内容以及与该对话关联的路线规划将一并删除，且无法恢复。",
      confirmLabel: "删除聊天",
    });

    if (!confirmed) return;

    ctx.deletingConversationId.value = item.id;

    try {
      await requestJson(`/api/chat/conversations/${item.id}`, { method: "DELETE" });

      ctx.conversations.value = ctx.conversations.value.filter(
        (conversation) => conversation.id !== item.id,
      );

      if (ctx.selectedConversationId.value === item.id) {
        ctx.selectedConversationId.value = null;
        ctx.messages.value = [];
      }

      ctx.showNotice("聊天已删除。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.deletingConversationId.value = null;
    }
  }

  function scrollChatToBottomSafely() {
    const workspace = ctx.chatWorkspaceRef.value;

    if (typeof workspace?.scheduleScrollToBottom === "function") {
      workspace.scheduleScrollToBottom();
      return;
    }

    if (typeof workspace?.scrollToBottom === "function") {
      workspace.scrollToBottom();
    }
  }

  async function savePartialExchange(conversationId, userMessage, partialAnswer) {
    if (!conversationId || !partialAnswer) return false;

    try {
      await requestJson(`/api/chat/conversations/${conversationId}/save-partial`, {
        method: "POST",
        body: JSON.stringify({
          userMessage,
          partialAnswer,
        }),
      });
      return true;
    } catch (error) {
      console.error("保存部分对话失败:", error);
      return false;
    }
  }

  async function sendMessage() {
    const text = String(ctx.chatInput.value || "").trim();

    if (!text || ctx.composerDisabled.value) {
      if (!ctx.isAuthenticated.value) ctx.openAccountModal("account");
      return;
    }

    const outgoingHistory = ctx.buildHistoryPayload();
    const persistedCountBeforeSend = ctx.messages.value.length;

    const optimisticUserMessage = createChatMessage({
      role: "user",
      content: text,
      createdAt: new Date().toISOString(),
    });

    const assistantPlaceholder = createChatMessage({
      role: "assistant",
      content: "",
      createdAt: new Date().toISOString(),
      suggestRoute: false,
      routeHint: "",
      usedWeatherContext: false,
      usedAttractionStatusContext: false,
      sourceInfo: null,
      sources: [],
      references: [],
      streaming: true,
    });

    ctx.sendingChat.value = true;
    ctx.messages.value = [...ctx.messages.value, optimisticUserMessage, assistantPlaceholder];
    resetChatInput();

    await nextTick();
    scrollChatToBottomSafely();

    let finalEvent = null;
    let recoveryAttempted = false;

    async function keepInterruptedExchange(errorMessage) {
      assistantPlaceholder.streaming = false;
      assistantPlaceholder.recovering = false;

      if (!assistantPlaceholder.content) {
        assistantPlaceholder.content = "连接中断，未能恢复完整回答，请重试。";
      }

      // 【关键修改】尝试保存未完成的对话到后端
      const conversationId = finalEvent?.conversationId || ctx.selectedConversationId.value;
      if (conversationId && assistantPlaceholder.content) {
        const saved = await savePartialExchange(conversationId, text, assistantPlaceholder.content);

        if (saved) {
          ctx.showNotice("对话已保存，但回答可能不完整。", "warning");
          await loadConversations();
          return;
        }
      }

      ctx.showNotice(errorMessage || "流式传输失败，请稍后重试。", "error");
    }

    async function tryRecoverInterruptedStream(errorMessage) {
      recoveryAttempted = true;
      const recoveryConversationId = finalEvent?.conversationId || ctx.selectedConversationId.value;
      const canRecover = Boolean(recoveryConversationId);

      if (!canRecover) {
        throw new Error(errorMessage || "流式传输失败，请稍后重试。");
      }

      assistantPlaceholder.streaming = false;
      assistantPlaceholder.recovering = true;
      ctx.showNotice("流式传输已中断，请等待回答全部完成", "info");

      const recovered = await recoverInterruptedChatStream(
        recoveryConversationId,
        persistedCountBeforeSend,
      );

      assistantPlaceholder.recovering = false;

      if (!recovered) {
        throw new Error(errorMessage || "流式传输失败，请稍后重试。");
      }
    }

    try {
      let streamError = "";

      const abortController = new AbortController();
      activeChatAbortController = abortController;

      await requestNdjsonStream(
        "/api/chat/stream",
        {
          method: "POST",
          signal: abortController.signal,
          body: JSON.stringify({
            message: text,
            history: outgoingHistory,
            conversationId: ctx.selectedConversationId.value,
            provider: ctx.provider.value,
            model: ctx.model.value,
            location: ctx.buildChatLocationPayload?.(),
          }),
        },
        async (event) => {
          if (!event || typeof event !== "object") return;

          if (event.type === "meta" && typeof event.conversationId === "number") {
            ctx.selectedConversationId.value = event.conversationId;
            return;
          }

          if (event.type === "delta") {
            assistantPlaceholder.content += event.content || "";
            await nextTick();
            scrollChatToBottomSafely();
            return;
          }

          if (event.type === "done") {
            finalEvent = event;

            assistantPlaceholder.content =
              event.answer || assistantPlaceholder.content || "没有收到模型回复。";
            assistantPlaceholder.createdAt = new Date().toISOString();
            assistantPlaceholder.suggestRoute = Boolean(event.suggestRoute);
            assistantPlaceholder.routeHint = event.routeHint || "";
            assistantPlaceholder.usedWeatherContext = Boolean(event.usedWeatherContext);
            assistantPlaceholder.usedAttractionStatusContext = Boolean(
              event.usedAttractionStatusContext,
            );
            assistantPlaceholder.sourceInfo = event.sourceInfo || null;
            assistantPlaceholder.sources = Array.isArray(event.sources) ? event.sources : [];
            assistantPlaceholder.references = Array.isArray(event.references)
              ? event.references
              : [];
            assistantPlaceholder.streaming = false;

            if (typeof event.conversationId === "number") {
              ctx.selectedConversationId.value = event.conversationId;
            }

            await nextTick();
            scrollChatToBottomSafely();
            return;
          }

          if (event.type === "error") {
            streamError = event.message || "流式回答失败，请稍后重试。";
          }
        },
      );

      if (streamError || !finalEvent) {
        await tryRecoverInterruptedStream(streamError);
        return;
      }

      assistantPlaceholder.streaming = false;

      if (!assistantPlaceholder.content && finalEvent?.answer) {
        assistantPlaceholder.content = finalEvent.answer;
      }

      await loadConversations();
      await nextTick();
      scrollChatToBottomSafely();
    } catch (error) {
      assistantPlaceholder.streaming = false;

      if (error?.name === "AbortError") {
        // 如果没有任何内容，直接移除占位符
        if (!assistantPlaceholder.content) {
          ctx.messages.value = ctx.messages.value.filter(
            (message) => message !== assistantPlaceholder,
          );

          if (ctx.selectedConversationId.value) {
            await loadConversations();
          }

          ctx.showNotice("已停止生成。", "info");
          return;
        }

        // 如果有部分内容，保存到后端
        const conversationId = finalEvent?.conversationId || ctx.selectedConversationId.value;
        if (conversationId) {
          const saved = await savePartialExchange(conversationId, text, assistantPlaceholder.content);

          if (saved) {
            // 重新加载当前对话的消息，避免重复
            try {
              const detail = await requestJson(`/api/chat/conversations/${conversationId}`);
              ctx.messages.value = mapConversationMessages(detail);
              await loadConversations();
              ctx.showNotice("已停止生成，部分回答已保存。", "info");
              return;
            } catch (reloadError) {
              // 重新加载失败，至少移除占位符
              ctx.messages.value = ctx.messages.value.filter(
                (message) => message !== assistantPlaceholder,
              );
            }
          }
        }

        // 保存失败，显示警告
        ctx.showNotice("已停止生成，但保存失败。", "warning");
        return;
      }

      const recoveryErrorMessage = error?.message || "流式传输失败，请稍后重试。";

      if (recoveryAttempted) {
        await keepInterruptedExchange(recoveryErrorMessage);
        return;
      }

      try {
        await tryRecoverInterruptedStream(recoveryErrorMessage);
        return;
      } catch (recoveryError) {
        await keepInterruptedExchange(recoveryError.message);
      }
    } finally {
      activeChatAbortController = null;
      ctx.sendingChat.value = false;
    }
  }

  function stopStreamingResponse() {
    activeChatAbortController?.abort();
  }

  return {
    resetChatInput,
    createChatMessage,
    mapConversationMessages,
    sleep,
    recoverInterruptedChatStream,
    startNewChat,
    useStarterPrompt,
    startFreshChatWithPrompt,
    loadConversations,
    selectConversation,
    deleteConversation,
    sendMessage,
    stopStreamingResponse,
  };
}