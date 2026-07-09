import { requestJson, requestNdjsonStream } from "./request";

export const chatApi = {
  listConversations() {
    return requestJson("/api/chat/conversations");
  },
  getConversation(id) {
    return requestJson(`/api/chat/conversations/${id}`);
  },
  deleteConversation(id) {
    return requestJson(`/api/chat/conversations/${id}`, { method: "DELETE" });
  },
  stream(payload, onEvent, signal) {
    return requestNdjsonStream(
      "/api/chat/stream",
      { method: "POST", body: JSON.stringify(payload), signal },
      onEvent,
    );
  },
};
