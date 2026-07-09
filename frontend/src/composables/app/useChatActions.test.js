import assert from "node:assert/strict";
import test from "node:test";

import { createChatActions } from "./useChatActions.js";

function ref(value) {
  return { value };
}

function createCtx() {
  return {
    chatInput: ref("推荐北京两日游"),
    composerDisabled: ref(false),
    isAuthenticated: ref(true),
    selectedConversationId: ref(null),
    messages: ref([]),
    sendingChat: ref(false),
    conversations: ref([]),
    loadingConversations: ref(false),
    deletingConversationId: ref(null),
    provider: ref(null),
    model: ref(null),
    chatWorkspaceRef: ref({
      scrollToBottom() {},
      scheduleScrollToBottom() {},
      focusComposer() {},
    }),
    notices: [],
    buildHistoryPayload: () => [],
    buildChatLocationPayload: () => null,
    showNotice(message, type) {
      this.notices.push({ message, type });
    },
    openAccountModal() {},
    switchView() {},
    closeMobileSidebar() {},
    requestConfirm: async () => true,
  };
}

function jsonResponse(body) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

test("sendMessage recovers the same answer after stream read failure", async () => {
  globalThis.window = { setTimeout: (callback) => globalThis.setTimeout(callback, 0) };

  const ctx = createCtx();
  const actions = createChatActions(ctx);
  let conversationPolls = 0;

  globalThis.fetch = async (url) => {
    if (url === "/api/chat/stream") {
      const encoder = new TextEncoder();
      let reads = 0;
      return new Response(
        new ReadableStream({
          pull(controller) {
            reads += 1;
            if (reads === 1) {
              controller.enqueue(encoder.encode('{"type":"meta","conversationId":10}\n'));
              return;
            }

            controller.error(new Error("network disconnected"));
          },
        }),
        { status: 200 },
      );
    }

    if (url === "/api/chat/conversations/10") {
      conversationPolls += 1;
      return jsonResponse({
        messages: [
          { role: "user", content: "推荐北京两日游", createdAt: "2026-05-11T00:00:00Z" },
          { role: "assistant", content: "完整回答", createdAt: "2026-05-11T00:00:01Z" },
        ],
      });
    }

    if (url === "/api/chat/conversations") {
      return jsonResponse({ conversations: [] });
    }

    throw new Error(`unexpected url: ${url}`);
  };

  await actions.sendMessage();

  assert.equal(conversationPolls, 1);
  assert.equal(ctx.messages.value.length, 2);
  assert.equal(ctx.messages.value[1].role, "assistant");
  assert.equal(ctx.messages.value[1].content, "完整回答");
  assert.equal(ctx.messages.value[1].streaming, false);
  assert.equal(ctx.messages.value[1].recovering, false);
  assert.equal(ctx.sendingChat.value, false);
});

test("sendMessage keeps the interrupted exchange when recovery times out", async () => {
  const originalDateNow = Date.now;
  let now = 0;
  Date.now = () => now;
  globalThis.window = {
    setTimeout(callback, ms) {
      now += ms;
      return globalThis.setTimeout(callback, 0);
    },
  };

  try {
    const ctx = createCtx();
    const actions = createChatActions(ctx);
    let conversationPolls = 0;

    globalThis.fetch = async (url) => {
      if (url === "/api/chat/stream") {
        const encoder = new TextEncoder();
        let reads = 0;
        return new Response(
          new ReadableStream({
            pull(controller) {
              reads += 1;
              if (reads === 1) {
                controller.enqueue(encoder.encode('{"type":"meta","conversationId":10}\n'));
                return;
              }

              controller.error(new Error("network disconnected"));
            },
          }),
          { status: 200 },
        );
      }

      if (url === "/api/chat/conversations/10") {
        conversationPolls += 1;
        return jsonResponse({
          messages: [
            { role: "user", content: "推荐北京两日游", createdAt: "2026-05-11T00:00:00Z" },
          ],
        });
      }

      throw new Error(`unexpected url: ${url}`);
    };

    await actions.sendMessage();

    assert.equal(conversationPolls, 30);
    assert.equal(ctx.messages.value.length, 2);
    assert.equal(ctx.messages.value[0].role, "user");
    assert.equal(ctx.messages.value[0].content, "推荐北京两日游");
    assert.equal(ctx.messages.value[1].role, "assistant");
    assert.equal(ctx.messages.value[1].content, "连接中断，未能恢复完整回答，请重试。");
    assert.equal(ctx.messages.value[1].streaming, false);
    assert.equal(ctx.messages.value[1].recovering, false);
    assert.equal(ctx.sendingChat.value, false);
  } finally {
    Date.now = originalDateNow;
  }
});
