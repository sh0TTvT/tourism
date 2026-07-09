import { requestJson } from "../../api/request";
import { cloneSharedRoute } from "../../utils/exploreFeed";
import { clamp, normalizeRoutePoints } from "../../utils/routePlanner";

export function createExploreActions(ctx) {
  async function loadExplorePosts() {
    if (!ctx.isAuthenticated.value) {
      ctx.explorePosts.value = [];
      return;
    }
    ctx.loadingExplorePosts.value = true;
    try {
      const data = await requestJson("/api/explore/posts");
      ctx.explorePosts.value = Array.isArray(data) ? data : [];
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.loadingExplorePosts.value = false;
    }
  }

  async function openExploreView(tab = "discover") {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return false;
    }
    if (!ctx.shouldKeepExploreRouteDetail(tab, ctx.exploreMineView.value, ctx.exploreMineCategory.value)) {
      ctx.exploreRoutePlanDetail.value = null;
    }
    ctx.exploreTab.value = tab;
    ctx.exploreNavigationMarker.value += 1;
    ctx.switchView("explore");
    const tasks = [loadExplorePosts()];
    if (ctx.shouldLoadExploreRoutePlans(tab, ctx.exploreMineView.value, ctx.exploreMineCategory.value)) {
      tasks.push(ctx.loadRoutePlans());
    }
    await Promise.all(tasks);
    return true;
  }

  function setExploreTab(tab) {
    if (!["discover", "mine", "routes", "map"].includes(tab)) return;
    if (!ctx.shouldKeepExploreRouteDetail(tab, ctx.exploreMineView.value, ctx.exploreMineCategory.value)) {
      ctx.exploreRoutePlanDetail.value = null;
    }
    ctx.exploreTab.value = tab;
    ctx.exploreNavigationMarker.value += 1;
    if (ctx.currentView.value !== "explore") {
      void openExploreView(tab);
      return;
    }
    if (ctx.shouldLoadExploreRoutePlans(tab, ctx.exploreMineView.value, ctx.exploreMineCategory.value) && !ctx.loadingRoutePlans.value) {
      void ctx.loadRoutePlans();
    }
    if (!ctx.loadingExplorePosts.value && !ctx.explorePosts.value.length) {
      void loadExplorePosts();
    }
  }

  function setExploreMineView(view) {
    if (!["mine", "favorites"].includes(view)) return;
    ctx.exploreMineView.value = view;
    if (!ctx.shouldKeepExploreRouteDetail(ctx.exploreTab.value, view, ctx.exploreMineCategory.value)) {
      ctx.exploreRoutePlanDetail.value = null;
    }
    ctx.exploreNavigationMarker.value += 1;
    if (ctx.currentView.value === "explore" && ctx.shouldLoadExploreRoutePlans(ctx.exploreTab.value, view, ctx.exploreMineCategory.value) && !ctx.loadingRoutePlans.value) {
      void ctx.loadRoutePlans();
    }
  }

  function setExploreMineCategory(category) {
    if (!["posts", "routes"].includes(category)) return;
    ctx.exploreMineCategory.value = category;
    if (!ctx.shouldKeepExploreRouteDetail(ctx.exploreTab.value, ctx.exploreMineView.value, category)) {
      ctx.exploreRoutePlanDetail.value = null;
    }
    ctx.exploreNavigationMarker.value += 1;
    if (ctx.currentView.value === "explore" && ctx.shouldLoadExploreRoutePlans(ctx.exploreTab.value, ctx.exploreMineView.value, category) && !ctx.loadingRoutePlans.value) {
      void ctx.loadRoutePlans();
    }
  }

  function leaveExplore() {
    ctx.switchView(ctx.previousPrimaryView.value || "chat");
  }

  function prependExplorePost(post) {
    ctx.explorePosts.value = [post, ...ctx.explorePosts.value.filter((item) => item.id !== post.id)];
  }

  function replaceExplorePost(post) {
    ctx.explorePosts.value = ctx.explorePosts.value.map((item) => (item.id === post.id ? post : item));
  }

  async function openExploreRoutePlanDetail(routePlanId) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const normalizedId = Number(routePlanId) || 0;
    if (!normalizedId) return;
    ctx.loadingExploreRoutePlanDetail.value = true;
    try {
      const data = await requestJson(`/api/routes/${normalizedId}`);
      ctx.exploreRoutePlanDetail.value = data;
    } catch (error) {
      ctx.exploreRoutePlanDetail.value = null;
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.loadingExploreRoutePlanDetail.value = false;
    }
  }

  function buildExploreSharedRoutePayload(plan) {
    const days = clamp(Number(plan?.days) || 3, 1, 14);
    return {
      title: String(plan?.title || "").trim(),
      summary: String(plan?.summary || "").trim(),
      destination: String(plan?.destination || "").trim(),
      days,
      interests: String(plan?.interests || "").trim(),
      budget: String(plan?.budget || "").trim(),
      departure: String(plan?.departure || "").trim(),
      tips: Array.isArray(plan?.tips) ? plan.tips.map((item) => String(item || "").trim()).filter(Boolean) : [],
      points: normalizeRoutePoints(plan?.points || [], days).map((point) => ({
        day: point.day,
        order: point.order,
        name: String(point.name || "").trim(),
        description: String(point.description || "").trim(),
        latitude: point.latitude,
        longitude: point.longitude,
      })),
    };
  }

  async function publishExplorePost(payload) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const title = String(payload?.title || "").trim();
    const content = String(payload?.content || "").trim();
    const imageUrls = Array.isArray(payload?.imageUrls) ? payload.imageUrls.filter(Boolean) : [];
    if (!title || !content) return;
    try {
      const data = await requestJson("/api/explore/posts", { method: "POST", body: JSON.stringify({ title, content, imageUrls }) });
      prependExplorePost(data);
      ctx.exploreTab.value = "discover";
      ctx.lastPublishedExplorePostId.value = data?.id ?? null;
      ctx.explorePublishedMarker.value += 1;
      ctx.showNotice("帖子已发布。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function shareRoutePlanToExplore(routePlanId) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const normalizedId = Number(routePlanId) || 0;
    if (!normalizedId || ctx.sharingRoutePlanId.value) return;
    const targetPlan = ctx.routePlans.value.find((item) => item.routePlanId === normalizedId);
    const label = targetPlan?.title || targetPlan?.destination || "这条路线";
    const confirmed = await ctx.requestConfirm({
      title: "分享路线到探索社区",
      message: `确定将"${label}"分享到探索社区吗？分享后其他用户可以看到这条路线并可导入使用。`,
      confirmLabel: "分享路线",
    });
    if (!confirmed) return;
    ctx.sharingRoutePlanId.value = normalizedId;
    try {
      const detail = await requestJson(`/api/routes/${normalizedId}`);
      const route = buildExploreSharedRoutePayload(detail);
      if (!route.destination || !route.points.length) {
        ctx.showNotice("这条路线数据不完整，暂时无法分享。", "error");
        return;
      }
      const content = route.summary
        ? `分享一条我整理的 ${route.destination} 路线，欢迎直接导入使用。\n\n${route.summary}`
        : `分享一条我整理的 ${route.destination} 路线，欢迎直接导入使用。`;
      const data = await requestJson("/api/explore/posts", {
        method: "POST",
        body: JSON.stringify({ title: route.title || `${route.destination} 路线`, content, route }),
      });
      prependExplorePost(data);
      ctx.exploreTab.value = "routes";
      ctx.showNotice("路线已分享到社区。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.sharingRoutePlanId.value = null;
    }
  }

  async function recordExploreRouteClick(postId) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const normalizedId = Number(postId) || 0;
    if (!normalizedId) return;
    try {
      const data = await requestJson(`/api/explore/posts/${normalizedId}/route-click`, { method: "PUT" });
      replaceExplorePost(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function toggleExploreLike(postId) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const target = ctx.explorePosts.value.find((post) => post.id === postId);
    if (!target) return;
    try {
      const data = await requestJson(`/api/explore/posts/${postId}/like`, { method: target.liked ? "DELETE" : "PUT" });
      replaceExplorePost(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function toggleExploreFavorite(postId) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const target = ctx.explorePosts.value.find((post) => post.id === postId);
    if (!target) return;
    try {
      const data = await requestJson(`/api/explore/posts/${postId}/favorite`, { method: target.favorited ? "DELETE" : "PUT" });
      replaceExplorePost(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function addExploreComment(payload) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const content = String(payload?.content || "").trim();
    if (!content) return;
    try {
      const data = await requestJson(`/api/explore/posts/${payload.postId}/comments`, { method: "POST", body: JSON.stringify({ content }) });
      replaceExplorePost(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function toggleExploreCommentLike(payload) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const postId = Number(payload?.postId) || 0;
    const commentId = Number(payload?.commentId) || 0;
    if (!postId || !commentId) return;
    const targetPost = ctx.explorePosts.value.find((post) => post.id === postId);
    const targetComment = targetPost?.comments?.find((comment) => comment.id === commentId);
    if (!targetComment) return;
    try {
      const data = await requestJson(`/api/explore/posts/${postId}/comments/${commentId}/like`, { method: targetComment.liked ? "DELETE" : "PUT" });
      replaceExplorePost(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function deleteExplorePost(post) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const postId = Number(post?.id) || 0;
    if (!postId) return;
    const title = String(post?.title || post?.route?.title || "未命名帖子").trim();
    const confirmed = await ctx.requestConfirm({ title: `删除“${title}”`, message: "删除后帖子正文、附带路线和相关互动都无法恢复。", confirmLabel: "删除帖子" });
    if (!confirmed) return;
    try {
      await requestJson(`/api/explore/posts/${postId}`, { method: "DELETE" });
      ctx.explorePosts.value = ctx.explorePosts.value.filter((item) => item.id !== postId);
      if (ctx.lastPublishedExplorePostId.value === postId) ctx.lastPublishedExplorePostId.value = null;
      ctx.showNotice("帖子已删除。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function deleteExploreComment(payload) {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const postId = Number(payload?.postId) || 0;
    const commentId = Number(payload?.commentId) || 0;
    if (!postId || !commentId) return;
    const preview = String(payload?.content || "").trim().slice(0, 24);
    const label = preview ? `“${preview}${preview.length >= 24 ? "..." : ""}”` : "这条评论";
    const confirmed = await ctx.requestConfirm({ title: `删除${label}`, message: "删除后这条评论及其互动无法恢复。", confirmLabel: "删除评论" });
    if (!confirmed) return;
    try {
      await requestJson(`/api/explore/posts/${postId}/comments/${commentId}`, { method: "DELETE" });
      await loadExplorePosts();
      ctx.showNotice("评论已删除。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  async function importSharedRoute(post) {
    if (!post?.route) return;
    if (!(await ctx.confirmDiscardRouteChanges())) return;
    if (!(await ctx.openRoutesView())) return;
    const route = cloneSharedRoute(post.route);
    ctx.resetRouteEditor({
      ...route,
      routePlanId: null,
      conversationId: null,
      title: route.title ? `${route.title} · 导入` : `${route.destination} 路线 · 导入`,
    });
    if (post?.id) {
      try {
        const data = await requestJson(`/api/explore/posts/${post.id}/route-apply`, { method: "PUT" });
        replaceExplorePost(data);
      } catch {}
    }
    ctx.showNotice("已导入到我的路线，继续编辑后即可保存。", "success");
  }

  async function startPointChat(payload) {
    const latitude = Number(payload?.latitude);
    const longitude = Number(payload?.longitude);
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return;
    const note = String(payload?.note || "").trim();
    const prompt = note
      ? `请围绕经纬度 ${latitude.toFixed(5)}, ${longitude.toFixed(5)} 这个点帮我开启一段新的旅行规划，${note}。请先判断适合怎么玩，再给出建议。`
      : `请围绕经纬度 ${latitude.toFixed(5)}, ${longitude.toFixed(5)} 这个点帮我开启一段新的旅行规划，优先考虑步行可达、适合停留和继续展开路线的地点。`;
    await ctx.startFreshChatWithPrompt(prompt, { autoSend: true });
  }

  function shouldLoadExploreRoutePlans(tab = ctx.exploreTab.value, mineView = ctx.exploreMineView.value, mineCategory = ctx.exploreMineCategory.value) {
    return tab === "routes" || (tab === "mine" && mineView === "mine" && mineCategory === "routes");
  }

  function shouldKeepExploreRouteDetail(tab = ctx.exploreTab.value, mineView = ctx.exploreMineView.value, mineCategory = ctx.exploreMineCategory.value) {
    return shouldLoadExploreRoutePlans(tab, mineView, mineCategory);
  }

  return {
    loadExplorePosts,
    openExploreView,
    setExploreTab,
    setExploreMineView,
    setExploreMineCategory,
    leaveExplore,
    prependExplorePost,
    replaceExplorePost,
    openExploreRoutePlanDetail,
    buildExploreSharedRoutePayload,
    publishExplorePost,
    shareRoutePlanToExplore,
    recordExploreRouteClick,
    toggleExploreLike,
    toggleExploreFavorite,
    addExploreComment,
    toggleExploreCommentLike,
    deleteExplorePost,
    deleteExploreComment,
    importSharedRoute,
    startPointChat,
    shouldLoadExploreRoutePlans,
    shouldKeepExploreRouteDetail,
  };
}
