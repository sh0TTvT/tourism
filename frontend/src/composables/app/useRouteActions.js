import { requestJson } from "../../api/request";
import {
  addDaysToDate,
  buildRouteDraftFromMessages,
  buildRouteDraftFromText,
  buildRouteSavePayload,
  clamp,
  createRouteEditor,
  normalizeRoutePoints,
  serializeRouteEditor,
} from "../../utils/routePlanner";

const timeSlots = [
  "08:30-10:30",
  "10:45-12:15",
  "13:30-15:30",
  "15:45-17:30",
  "19:00-21:00",
];

function safeMarkdown(value, fallback = "未填写") {
  const text = String(value || "").trim();
  return text || fallback;
}

function sanitizeFilename(value) {
  return safeMarkdown(value, "旅行路线")
    .replace(/[\\/:*?"<>|]/g, "-")
    .replace(/\s+/g, "-")
    .slice(0, 80);
}

function routeDateForDay(route, day) {
  return addDaysToDate(route.startDate, Math.max(Number(day) || 1, 1) - 1);
}

function buildTransportAdvice(route, point, index) {
  if (index === 0) {
    return route.departure
      ? `从 ${route.departure} 前往 ${route.destination}，建议优先选择高铁/飞机等城际交通，到达后换乘地铁、公交或网约车。`
      : "建议根据出发地选择高铁、飞机或自驾，到达后使用地铁、公交或网约车衔接。";
  }
  const name = point?.name ? `前往 ${point.name}` : "前往下一站";
  return `${name} 建议按实际距离选择步行、地铁/公交或网约车，跨城区点位优先使用地铁或打车。`;
}

function forecastForDate(weatherForecast, date) {
  return weatherForecast?.forecasts?.find((item) => item.date === date) || null;
}

function formatWeatherForecast(item) {
  if (!item) {
    return "出发前查看当日天气，雨天准备雨具并减少户外停留。";
  }
  const temp = [item.minTemperature, item.maxTemperature]
    .filter((value) => value != null)
    .join("-");
  const rain = item.precipitationProbabilityMax == null ? "" : `，降水概率 ${item.precipitationProbabilityMax}%`;
  return `${item.weatherDescription || "天气待确认"}${temp ? `，气温 ${temp}℃` : ""}${rain}`;
}

function buildRouteMarkdown(route, weatherForecast = null) {
  const days = clamp(Number(route.days) || 1, 1, 14);
  const points = normalizeRoutePoints(route.points || [], days);
  const grouped = Array.from({ length: days }, (_, index) => ({
    day: index + 1,
    points: points.filter((point) => point.day === index + 1),
  }));
  const tips = Array.isArray(route.tips) ? route.tips.filter(Boolean) : [];
  const dateRange = `${safeMarkdown(route.startDate)} 至 ${safeMarkdown(route.endDate)}`;

  const lines = [
    `# ${safeMarkdown(route.title, `${safeMarkdown(route.destination, "目的地")}旅行攻略`)}`,
    "",
    "## 路线概览",
    `- 起点：${safeMarkdown(route.departure)}`,
    `- 终点：${safeMarkdown(route.destination)}`,
    `- 日期：${dateRange}`,
    `- 行程天数：${days} 天`,
    `- 预算：${safeMarkdown(route.budget, "按实际消费安排")}`,
    `- 兴趣偏好：${safeMarkdown(route.interests, "综合游览")}`,
    `- 路线摘要：${safeMarkdown(route.summary, "暂无摘要")}`,
    "",
    "## 天气与准备",
    `- 天气：${weatherForecast?.source ? `已尝试读取 ${safeMarkdown(route.destination)} 天气；` : ""}请在出发前 1-3 天复核目的地天气预报；如遇降雨、高温、寒潮或大风，优先调整户外点位顺序。`,
    "- 装备：携带身份证件、充电宝、常用药、雨具、防晒用品和舒适步行鞋。",
    "- 预约：热门景区、博物馆、演出和餐厅建议提前预约，并保存预约码或订单截图。",
    "",
    "## 出行方式",
    `- 城际交通：${route.departure ? `从 ${route.departure} 到 ${safeMarkdown(route.destination)} 可优先比较高铁、飞机和自驾耗时。` : "请根据实际出发地比较高铁、飞机和自驾耗时。"}`,
    "- 市内交通：优先使用地铁/公交覆盖主城区点位；夜间、跨区或携带行李时可使用网约车。",
    "- 点位衔接：相邻点位距离较近时步行，超过 3 公里建议公共交通或打车。",
    "",
    "## 每日行程",
  ];

  grouped.forEach((group) => {
    const currentDate = routeDateForDay(route, group.day);
    const currentWeather = formatWeatherForecast(forecastForDate(weatherForecast, currentDate));
    lines.push("", `### Day ${group.day} · ${currentDate || "未设置日期"}`);
    if (!group.points.length) {
      lines.push("- 暂无点位，可补充景点、餐饮或交通安排。");
      return;
    }
    group.points.forEach((point, index) => {
      lines.push(
        "",
        `#### ${index + 1}. ${safeMarkdown(point.name, "未命名点位")}`,
        `- 时间：${timeSlots[index] || "自由安排"}`,
        `- 地点：${safeMarkdown(point.name, "未命名点位")}`,
        `- 安排：${safeMarkdown(point.description, "根据现场情况安排游览。")}`,
        `- 天气：${currentWeather}`,
        `- 出行方式：${buildTransportAdvice(route, point, index)}`,
      );
    });
  });

  lines.push("", "## 注意事项");
  (tips.length ? tips : ["出行前确认天气与景点开放时间", "热门景点建议提前预约", "预留机动时间应对交通和闭馆变化"])
    .forEach((tip) => {
      lines.push(`- ${tip}`);
    });
  lines.push(
    "",
    "## 应急与备选",
    "- 如遇天气变化：将户外景点替换为博物馆、商场、咖啡馆或室内展馆。",
    "- 如遇排队拥堵：优先保留核心点位，压缩打卡型点位停留时间。",
    "- 如遇交通延误：减少跨区移动，把同一区域点位合并游览。",
  );

  return `${lines.join("\n")}\n`;
}

function downloadMarkdown(route, weatherForecast = null) {
  const markdown = buildRouteMarkdown(route, weatherForecast);
  const blob = new Blob([markdown], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `${sanitizeFilename(route.title || route.destination)}攻略.md`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

export function createRouteActions(ctx) {
  function applyRouteResponse(payload) {
    const next = createRouteEditor(payload, ctx.preferenceForm.value);
    ctx.routeEditor.value = next;
    ctx.activeRoutePlanId.value = next.routePlanId;
    ctx.routeSnapshot.value = serializeRouteEditor(next);
  }

  function resetRouteEditor(overrides = {}) {
    const next = createRouteEditor(overrides, ctx.preferenceForm.value);
    ctx.routeEditor.value = next;
    ctx.activeRoutePlanId.value = next.routePlanId;
    ctx.routeSnapshot.value = serializeRouteEditor(next);
  }

  async function confirmDiscardRouteChanges() {
    if (!ctx.routeDirty.value) return true;
    return ctx.requestConfirm({
      title: "放弃修改",
      message: "当前路线有未保存修改，确定放弃这些修改吗？",
      confirmLabel: "放弃修改",
    });
  }

  async function loadRoutePlans() {
    if (!ctx.isAuthenticated.value) {
      ctx.routePlans.value = [];
      return;
    }
    ctx.loadingRoutePlans.value = true;
    try {
      const data = await requestJson("/api/routes");
      ctx.routePlans.value = Array.isArray(data) ? data : [];
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.loadingRoutePlans.value = false;
    }
  }

  async function openRoutesView() {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return false;
    }
    ctx.switchView("routes");
    await loadRoutePlans();
    return true;
  }

  async function createRouteDraft() {
    if (!(await confirmDiscardRouteChanges())) return;
    if (!(await openRoutesView())) return;
    resetRouteEditor({ conversationId: ctx.selectedConversationId.value });
  }

  async function openRouteDraftFromMessage(message) {
    console.log("[DEBUG] openRouteDraftFromMessage 被调用, message:", message);

    if (!(await confirmDiscardRouteChanges())) {
      console.log("[DEBUG] 用户取消了丢弃更改");
      return;
    }

    if (!ctx.isAuthenticated.value) {
      console.log("[DEBUG] 用户未登录，打开登录弹窗");
      ctx.openAccountModal("account");
      return;
    }

    console.log("[DEBUG] 切换到路线视图");
    ctx.switchView("routes");

    const messages = buildHistoryPayload(message);
    console.log("[DEBUG] buildHistoryPayload 返回:", messages?.length, "条消息");

    // Pre-fill locally for instant visual feedback; no LLM call is required here.
    const text = message?.routeHint || message?.content || "";
    const draft = messages?.length
      ? buildRouteDraftFromMessages(messages, ctx.selectedConversationId.value, ctx.preferenceForm.value)
      : buildRouteDraftFromText(text, ctx.selectedConversationId.value, ctx.preferenceForm.value);

    console.log("[DEBUG] draft 内容:", {
      destination: draft.destination,
      days: draft.days,
      pointsCount: draft.points?.length || 0,
      points: draft.points?.map(p => ({ name: p.name, day: p.day, order: p.order }))
    });

    resetRouteEditor(draft);
    void loadRoutePlans();

    console.log("[DEBUG] resetRouteEditor 后, routeEditor.value:", {
      destination: ctx.routeEditor.value.destination,
      days: ctx.routeEditor.value.days,
      pointsCount: ctx.routeEditor.value.points?.length || 0,
      points: ctx.routeEditor.value.points?.map(p => ({ name: p.name, day: p.day, order: p.order }))
    });

    if (!ctx.routeEditor.value.destination.trim()) {
      console.log("[DEBUG] 目的地为空，显示通知并返回");
      ctx.showNotice("已打开路线草稿，请先补充目的地后再生成。", "info");
      return;
    }

    const hasPoints = Array.isArray(ctx.routeEditor.value.points) && ctx.routeEditor.value.points.length > 0;
    console.log("[DEBUG] hasPoints:", hasPoints, "points:", ctx.routeEditor.value.points);

    if (!hasPoints) {
      console.log("[DEBUG] 没有点位，显示通知并返回");
      ctx.showNotice("已从对话中提取基础信息，可补充点位或手动生成路线。", "info");
      return;
    }

    console.log("[DEBUG] 准备调用 enrichRoutePointCoordinates");
    ctx.showNotice("已从对话中提取路线，正在补全地图坐标...", "info");
    void enrichRoutePointCoordinates();
  }

  async function selectRoutePlan(item) {
    const routePlanId = item?.routePlanId;
    if (!routePlanId) return;
    if (!(await confirmDiscardRouteChanges())) return;
    if (!(await openRoutesView())) return;
    ctx.loadingRouteDetail.value = true;
    try {
      const data = await requestJson(`/api/routes/${routePlanId}`);
      applyRouteResponse(data);
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.loadingRouteDetail.value = false;
      ctx.closeMobileSidebar();
    }
  }

  async function deleteRoutePlan(item) {
    const routePostId = Number(item?.id) || 0;
    if (routePostId && item?.route) {
      const title = String(item?.route?.title || item?.title || "未命名路线").trim();
      const confirmed = await ctx.requestConfirm({
        title: `删除"${title}"`,
        message: "删除后帖子正文、附带路线和相关互动都无法恢复。",
        confirmLabel: "删除路线",
      });
      if (!confirmed) return;
      try {
        await requestJson(`/api/explore/posts/${routePostId}`, { method: "DELETE" });
        ctx.explorePosts.value = ctx.explorePosts.value.filter((post) => post.id !== routePostId);
        if (ctx.lastPublishedExplorePostId.value === routePostId) ctx.lastPublishedExplorePostId.value = null;
        ctx.showNotice("路线已删除。", "success");
      } catch (error) {
        ctx.showNotice(error.message, "error");
      }
      return;
    }

    const routePlanId = item?.routePlanId;
    if (!routePlanId) return;
    const title = item.title || "未命名路线";
    const confirmed = await ctx.requestConfirm({
      title: `删除"${title}"`,
      message: "删除后这条路线将无法恢复。",
      confirmLabel: "删除路线",
    });
    if (!confirmed) return;
    try {
      await requestJson(`/api/routes/${routePlanId}`, { method: "DELETE" });
      ctx.routePlans.value = ctx.routePlans.value.filter((plan) => plan.routePlanId !== routePlanId);
      if (ctx.exploreRoutePlanDetail.value?.routePlanId === routePlanId) {
        ctx.exploreRoutePlanDetail.value = null;
        if (ctx.currentView.value === "explore") ctx.exploreNavigationMarker.value += 1;
      }
      if (ctx.activeRoutePlanId.value === routePlanId) {
        resetRouteEditor({ conversationId: ctx.selectedConversationId.value });
      }
      ctx.showNotice("路线已删除。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    }
  }

  function buildHistoryPayload(sourceMessage = null) {
    const sourceIndex = sourceMessage
      ? ctx.messages.value.findIndex((message) => message === sourceMessage)
      : -1;
    const sourceMessages =
      sourceIndex >= 0
        ? ctx.messages.value.slice(Math.max(0, sourceIndex - 5), sourceIndex + 1)
        : ctx.messages.value;

    return sourceMessages.map((message) => ({ role: message.role, content: message.content }));
  }

  function markRouteDirty() {}

  async function enrichRoutePointCoordinates() {
    console.log("[DEBUG] enrichRoutePointCoordinates 开始");
    const route = ctx.routeEditor.value;
    const destination = String(route.destination || "").trim();
    const points = normalizeRoutePoints(route.points || [], route.days);

    console.log("[DEBUG] 待补全的点位:", points.map((p) => ({
      name: p.name,
      hasCoords: Number.isFinite(p.latitude) && Number.isFinite(p.longitude),
    })));

    const needsCoordinates = points.some(
      (point) => !Number.isFinite(point.latitude) || !Number.isFinite(point.longitude),
    );
    if (!destination || !points.length || !needsCoordinates) {
      console.log("[DEBUG] 跳过坐标补全 - destination:", destination, "points.length:", points.length, "needsCoordinates:", needsCoordinates);
      return;
    }
    try {
      console.log("[DEBUG] 调用 API /api/routes/geocode-points");
      const geocodedPoints = await requestJson("/api/routes/geocode-points", {
        method: "POST",
        body: JSON.stringify({ destination, points }),
      });

      console.log("[DEBUG] API 返回的点位:", geocodedPoints?.map((p) => ({
        name: p.name,
        lat: p.latitude,
        lon: p.longitude,
        hasCoords: Number.isFinite(p.latitude) && Number.isFinite(p.longitude),
      })));

      if (!Array.isArray(geocodedPoints) || geocodedPoints.length === 0) {
        console.error("[DEBUG] API 返回空数组或非数组");
        ctx.showNotice("坐标解析服务异常，请稍后重试或手动调整。", "error");
        return;
      }
      ctx.routeEditor.value.points = normalizeRoutePoints(geocodedPoints, ctx.routeEditor.value.days);

      console.log("[DEBUG] 更新后的 routeEditor.points:", ctx.routeEditor.value.points.map((p) => ({
        name: p.name,
        lat: p.latitude,
        lon: p.longitude,
        hasCoords: Number.isFinite(p.latitude) && Number.isFinite(p.longitude),
      })));

      // 统计成功和失败的点位
      const successCount = ctx.routeEditor.value.points.filter(
        (point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude),
      ).length;
      const totalCount = ctx.routeEditor.value.points.length;
      const failedCount = totalCount - successCount;

      console.log("[DEBUG] 统计结果 - 成功:", successCount, "失败:", failedCount, "总数:", totalCount);

      // 找出失败的点位名称
      const failedPoints = ctx.routeEditor.value.points
        .filter((point) => !Number.isFinite(point.latitude) || !Number.isFinite(point.longitude))
        .map((point) => point.name);

      // 显示详细反馈
      if (failedCount === 0) {
        ctx.showNotice(`已成功定位全部 ${successCount} 个点位。`, "success");
      } else if (successCount > 0) {
        const failedNames = failedPoints.slice(0, 3).map((name) => `"${name}"`).join("、");
        const moreText = failedPoints.length > 3 ? ` 等${failedPoints.length}个` : "";
        ctx.showNotice(
          `已定位 ${successCount} 个点位，${failedCount} 个点位需要手动调整（${failedNames}${moreText}）。`,
          "warning",
        );
      } else {
        ctx.showNotice("所有点位坐标解析失败，请检查点位名称是否准确，或手动在地图上调整。", "error");
      }
    } catch (error) {
      console.error("[DEBUG] enrichRoutePointCoordinates 失败:", error);
      ctx.showNotice("坐标解析服务异常，请稍后重试或手动调整。", "error");
    }
  }

  async function generateRoutePlan() {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const destination = String(ctx.routeEditor.value.destination || "").trim();
    if (!destination) {
      ctx.showNotice("请先填写目的地。", "error");
      return;
    }
    ctx.routeGenerating.value = true;
    ctx.switchView("routes");
    try {
      const data = await requestJson("/api/routes/plan", {
        method: "POST",
        body: JSON.stringify({
          destination,
          days: clamp(Number(ctx.routeEditor.value.days) || 3, 1, 14),
          startDate: ctx.routeEditor.value.startDate,
          endDate: ctx.routeEditor.value.endDate,
          interests: String(ctx.routeEditor.value.interests || "").trim(),
          budget: String(ctx.routeEditor.value.budget || "").trim(),
          departure: String(ctx.routeEditor.value.departure || "").trim(),
          conversationId: ctx.routeEditor.value.conversationId,
          provider: ctx.provider.value,
          model: ctx.model.value,
        }),
      });
      applyRouteResponse(data);
      await loadRoutePlans();
      ctx.showNotice("路线已生成。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.routeGenerating.value = false;
    }
  }

  async function createRoutePlan() {
    if (!ctx.isAuthenticated.value) {
      ctx.openAccountModal("account");
      return;
    }
    const destination = String(ctx.routeEditor.value.destination || "").trim();
    if (!destination) {
      ctx.showNotice("请先填写目的地。", "error");
      return;
    }
    let payload;
    try {
      payload = buildRouteSavePayload(ctx.routeEditor.value);
    } catch (error) {
      ctx.showNotice(error.message, "error");
      return;
    }
    payload.conversationId = ctx.routeEditor.value.conversationId;
    ctx.routeSaving.value = true;
    try {
      const data = await requestJson("/api/routes", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      applyRouteResponse(data);
      await loadRoutePlans();
      ctx.showNotice("路线已保存。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.routeSaving.value = false;
    }
  }

  async function saveRoutePlan() {
    if (!ctx.routeEditor.value.routePlanId) {
      const hasPoints = Array.isArray(ctx.routeEditor.value.points) && ctx.routeEditor.value.points.length > 0;
      if (hasPoints) {
        await createRoutePlan();
      } else {
        await generateRoutePlan();
      }
      return;
    }
    let payload;
    try {
      payload = buildRouteSavePayload(ctx.routeEditor.value);
    } catch (error) {
      ctx.showNotice(error.message, "error");
      return;
    }
    ctx.routeSaving.value = true;
    try {
      const data = await requestJson(`/api/routes/${ctx.routeEditor.value.routePlanId}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      applyRouteResponse(data);
      await loadRoutePlans();
      ctx.showNotice("路线已保存。", "success");
    } catch (error) {
      ctx.showNotice(error.message, "error");
    } finally {
      ctx.routeSaving.value = false;
    }
  }

  async function exportRoutePlan() {
    const route = ctx.routeEditor.value;
    if (!String(route.destination || "").trim()) {
      ctx.showNotice("请先填写目的地。", "error");
      return;
    }
    let weatherForecast = null;
    try {
      const params = new URLSearchParams({
        location: String(route.destination || "").trim(),
        date: route.startDate,
        days: String(Math.min(clamp(Number(route.days) || 1, 1, 14), 7)),
      });
      weatherForecast = await requestJson(`/api/weather/forecast?${params.toString()}`);
    } catch (error) {
      weatherForecast = null;
    }
    downloadMarkdown(route, weatherForecast);
    ctx.showNotice("攻略已导出。", "success");
  }

  function replaceRoutePoints(points) {
    ctx.routeEditor.value = {
      ...ctx.routeEditor.value,
      points: normalizeRoutePoints(points, ctx.routeEditor.value.days),
    };
  }

  function addRoutePoint(payload) {
    const maxDay = Math.max(Number(ctx.routeEditor.value.days) || 1, 1);
    const nextDay = clamp(
      Number(typeof payload === "object" ? payload?.day : payload) || 1,
      1,
      maxDay,
    );
    const rawLatitude = typeof payload === "object" ? payload?.latitude : null;
    const rawLongitude = typeof payload === "object" ? payload?.longitude : null;
    const latitude =
      rawLatitude === null || rawLatitude === undefined || rawLatitude === ""
        ? null
        : Number(rawLatitude);
    const longitude =
      rawLongitude === null || rawLongitude === undefined || rawLongitude === ""
        ? null
        : Number(rawLongitude);
    const points = normalizeRoutePoints(ctx.routeEditor.value.points, maxDay);
    const nextOrder = points.filter((point) => point.day === nextDay).length + 1;

    replaceRoutePoints([
      ...points,
      {
        day: nextDay,
        order: nextOrder,
        name: "",
        description: "",
        latitude: Number.isFinite(latitude) ? latitude : null,
        longitude: Number.isFinite(longitude) ? longitude : null,
      },
    ]);
  }

  function removeRoutePoint(index) {
    if (index == null || index < 0 || index >= ctx.routeEditor.value.points.length) return;
    const nextPoints = ctx.routeEditor.value.points.filter((_, pointIndex) => pointIndex !== index);
    replaceRoutePoints(nextPoints);
  }
  function moveRoutePoint(payload) {
    const sourceIndex = payload?.from;
    if (sourceIndex == null || sourceIndex < 0 || sourceIndex >= ctx.routeEditor.value.points.length) return;

    const maxDay = Math.max(Number(ctx.routeEditor.value.days) || 1, 1);
    const targetDay = clamp(
      Number(payload?.day) || ctx.routeEditor.value.points[sourceIndex]?.day || 1,
      1,
      maxDay,
    );
    const targetIndex = payload?.to;
    if (targetIndex === sourceIndex && targetDay === ctx.routeEditor.value.points[sourceIndex]?.day) {
      return;
    }

    const decoratedPoints = ctx.routeEditor.value.points.map((point, index) => ({
      ...point,
      originalIndex: index,
    }));
    const moved = {
      ...decoratedPoints[sourceIndex],
      day: targetDay,
    };

    const groups = new Map();
    decoratedPoints
      .filter((point) => point.originalIndex !== sourceIndex)
      .sort((left, right) => left.day - right.day || left.order - right.order)
      .forEach((point) => {
        if (!groups.has(point.day)) {
          groups.set(point.day, []);
        }
        groups.get(point.day).push(point);
      });

    const targetGroup = groups.get(targetDay) || [];
    const insertIndex =
      targetIndex == null
        ? targetGroup.length
        : targetGroup.findIndex((point) => point.originalIndex === targetIndex);
    targetGroup.splice(insertIndex >= 0 ? insertIndex : targetGroup.length, 0, moved);
    groups.set(targetDay, targetGroup);

    const nextPoints = [];
    for (let day = 1; day <= maxDay; day += 1) {
      (groups.get(day) || []).forEach((point, index) => {
        const { originalIndex, ...routePoint } = point;
        nextPoints.push({
          ...routePoint,
          day,
          order: index + 1,
        });
      });
    }
    replaceRoutePoints(nextPoints);
  }

  return {
    applyRouteResponse,
    resetRouteEditor,
    confirmDiscardRouteChanges,
    loadRoutePlans,
    openRoutesView,
    createRouteDraft,
    openRouteDraftFromMessage,
    selectRoutePlan,
    deleteRoutePlan,
    buildHistoryPayload,
    markRouteDirty,
    generateRoutePlan,
    createRoutePlan,
    saveRoutePlan,
    exportRoutePlan,
    addRoutePoint,
    removeRoutePoint,
    moveRoutePoint,
  };
}
