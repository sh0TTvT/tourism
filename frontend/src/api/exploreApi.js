import { requestJson } from "./request";

export const exploreApi = {
  listPosts() {
    return requestJson("/api/explore/posts");
  },
  createPost(payload) {
    return requestJson("/api/explore/posts", { method: "POST", body: JSON.stringify(payload) });
  },
  deletePost(id) {
    return requestJson(`/api/explore/posts/${id}`, { method: "DELETE" });
  },
  recordRouteClick(id) {
    return requestJson(`/api/explore/posts/${id}/route-click`, { method: "POST" });
  },
  applyRoute(id) {
    return requestJson(`/api/explore/posts/${id}/route-apply`, { method: "POST" });
  },
  toggleLike(id) {
    return requestJson(`/api/explore/posts/${id}/like`, { method: "POST" });
  },
  toggleFavorite(id) {
    return requestJson(`/api/explore/posts/${id}/favorite`, { method: "POST" });
  },
  addComment(id, payload) {
    return requestJson(`/api/explore/posts/${id}/comments`, { method: "POST", body: JSON.stringify(payload) });
  },
  toggleCommentLike(postId, commentId) {
    return requestJson(`/api/explore/posts/${postId}/comments/${commentId}/like`, { method: "POST" });
  },
  deleteComment(postId, commentId) {
    return requestJson(`/api/explore/posts/${postId}/comments/${commentId}`, { method: "DELETE" });
  },
};
