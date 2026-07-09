<template>
  <div class="route-workspace">
    <section class="route-detail">
      <header class="route-detail-head">
        <div class="route-history-menu" @click.stop>
          <button
            type="button"
            class="ghost-button route-history-trigger"
            @click="toggleHistoryRoutes"
          >
            历史路线
          </button>

          <section
            v-if="historyOpen"
            class="route-history-popover"
            aria-label="历史路线"
          >
            <div class="route-history-head">
              <div>
                <p>路线中心</p>
                <h2>我的路线</h2>
              </div>
              <button
                type="button"
                class="ghost-button compact"
                @click="createDraftFromHistory"
              >
                新建草稿
              </button>
            </div>

            <div v-if="!isAuthenticated" class="route-library-empty">
              <strong>登录后可保存和查看路线</strong>
              <span>路线会和聊天记录关联，方便后续继续调整。</span>
            </div>

            <div v-else-if="loadingPlans" class="route-library-empty">
              <strong>正在加载路线</strong>
              <span>稍后会展示你的历史路线和最近编辑内容。</span>
            </div>

            <div v-else-if="plans.length" class="route-history-content">
              <article
                v-for="item in plans"
                :key="item.routePlanId"
                class="route-library-card"
                :class="{
                  active: String(activePlanId) === String(item.routePlanId),
                }"
              >
                <button
                  type="button"
                  class="route-library-card-main"
                  @click="selectPlanFromHistory(item)"
                >
                  <strong>{{
                    item.title || `${item.destination} 路线`
                  }}</strong>
                  <span
                    >{{ item.destination }} ·
                    {{ routeDateRangeLabel(item) }}</span
                  >
                  <small>{{ item.summary || "暂无摘要" }}</small>
                </button>

                <button
                  type="button"
                  class="route-library-card-delete"
                  @click.stop="emit('delete-plan', item)"
                >
                  删除
                </button>
              </article>
            </div>

            <div v-else class="route-library-empty">
              <strong>还没有保存的路线</strong>
              <span>在聊天里点击“生成路线”，或在这里新建草稿开始规划。</span>
            </div>
          </section>
        </div>
      </header>

      <div class="route-detail-grid">
        <div
          class="route-editor-column"
          ref="routeEditorScrollRef"
          @dragover.prevent="handleEditorDragOver"
          @dragleave="handleEditorDragLeave"
          @drop.prevent="handleEditorDrop"
        >
          <section class="route-card route-parameters">
            <div class="route-card-head">
              <div>
                <p>规划参数</p>
                <h3>基础信息</h3>
              </div>
            </div>

            <div class="route-form-grid">
              <label>
                目的地
                <input
                  v-model="editor.destination"
                  type="text"
                  maxlength="120"
                  @input="markDirty"
                />
              </label>

              <label>
                标题
                <input
                  v-model="editor.title"
                  type="text"
                  maxlength="180"
                  @input="markDirty"
                />
              </label>

              <label class="route-date-range-field">
                起止日期
                <input
                  ref="dateRangeInputRef"
                  v-model="dateRangeInputText"
                  type="text"
                  inputmode="numeric"
                  placeholder="2026/04/12 - 2026/04/15"
                  spellcheck="false"
                  @change="commitDateRangeInput"
                  @blur="resetDateRangeInput"
                  @mouseenter="startDateRangeScroll"
                  @mouseleave="stopDateRangeScroll"
                  @focus="stopDateRangeScroll"
                />
              </label>

              <label>
                预算
                <input
                  v-model="editor.budget"
                  type="text"
                  maxlength="120"
                  @input="markDirty"
                />
              </label>

              <label>
                兴趣偏好
                <input
                  v-model="editor.interests"
                  type="text"
                  maxlength="200"
                  @input="markDirty"
                />
              </label>

              <label>
                出发地
                <input
                  v-model="editor.departure"
                  type="text"
                  maxlength="120"
                  @input="markDirty"
                />
              </label>
            </div>
          </section>

          <section class="route-card">
            <div class="route-card-head">
              <div>
                <p>概览说明</p>
                <h3>摘要与提醒</h3>
              </div>
              <button
                type="button"
                class="ghost-button compact"
                @click="addTip"
              >
                新增提醒
              </button>
            </div>

            <label class="route-stack-field">
              路线摘要
              <textarea
                v-model="editor.summary"
                rows="4"
                maxlength="8000"
                @input="markDirty"
              ></textarea>
            </label>

            <div class="route-tips">
              <label
                v-for="(tip, index) in editor.tips"
                :key="`tip-${index}`"
                class="route-tip-item"
              >
                <span>提醒 {{ index + 1 }}</span>
                <div class="route-inline-actions">
                  <input
                    v-model="editor.tips[index]"
                    type="text"
                    maxlength="120"
                    @input="markDirty"
                  />
                  <button
                    type="button"
                    class="ghost-button compact danger"
                    @click="removeTip(index)"
                  >
                    删除
                  </button>
                </div>
              </label>
            </div>
          </section>

          <section class="route-card route-points-card">
            <div class="route-card-head">
              <div>
                <p>逐日安排</p>
                <h3>行程点位</h3>
              </div>
            </div>

            <div class="route-days" :key="dragRenderKey">
              <section
                v-for="group in dayGroups"
                :key="`day-${group.day}`"
                class="route-day-section"
                @dragover.prevent="handleDayDragOver(group.day, $event)"
                @drop.prevent="dropIntoDay(group.day)"
              >
                <header class="route-day-head">
                  <div>
                    <strong>{{ dateLabelForDay(group.day) }}</strong>
                    <span>{{ group.points.length }} 个点位</span>
                  </div>
                  <button
                    type="button"
                    class="ghost-button compact"
                    :class="{ active: pendingAddDay === group.day }"
                    @click="handleAddPoint(group.day)"
                  >
                    {{ pendingAddDay === group.day ? "选择位置" : "新增点位" }}
                  </button>
                </header>

                <div v-if="group.points.length" class="route-point-list">
                  <template
                    v-for="point in group.points"
                    :key="`point-wrap-${group.day}-${point.flatIndex}`"
                  >
                    <div
                      v-if="isDropPlaceholder(point.flatIndex, group.day)"
                      class="route-point-placeholder"
                    >
                      <span>释放后插入到这里</span>
                    </div>

                    <article
                      class="route-point-card"
                      :data-route-point-day="group.day"
                      :data-route-point-index="point.flatIndex"
                      :class="{
                        'is-dragging-source':
                          draggingIndex === point.flatIndex && dragSourceHidden,
                        'no-coordinates': !hasCoordinates(point),
                        'is-pending-edit': pendingEditIndex === point.flatIndex,
                      }"
                      @dragover.prevent.stop="
                        handlePointDragOver(point.flatIndex, group.day, $event)
                      "
                      @dragenter.prevent.stop="
                        setDragTarget(point.flatIndex, group.day)
                      "
                      @drop.prevent.stop="
                        dropOnPoint(point.flatIndex, group.day)
                      "
                      @click="handlePointCardClick(point.flatIndex)"
                    >
                      <div
                        class="route-point-card-head route-point-drag-handle"
                        title="按住这里拖动排序"
                        draggable="true"
                        @dragstart.stop="startDrag(point.flatIndex, $event)"
                        @dragend.stop="endDrag"
                        @click.stop
                      >
                        <span class="route-point-badge">{{ point.order }}</span>
                        <strong>拖动排序</strong>
                        <button
                          v-if="!hasCoordinates(point)"
                          type="button"
                          class="ghost-button compact"
                          :class="{ active: pendingEditIndex === point.flatIndex }"
                          @click.stop="handleEditPointLocation(point.flatIndex)"
                        >
                          {{ pendingEditIndex === point.flatIndex ? "选择位置" : "添加坐标" }}
                        </button>
                        <button
                          type="button"
                          class="ghost-button compact danger"
                          @click.stop="emit('remove-point', point.flatIndex)"
                        >
                          删除
                        </button>
                      </div>

                      <div class="route-form-grid route-point-grid">
                        <label>
                          点位名称
                          <input
                            v-model="editor.points[point.flatIndex].name"
                            type="text"
                            maxlength="150"
                            data-route-point-name-input
                            @input="markDirty"
                          />
                        </label>
                      </div>

                      <label class="route-stack-field">
                        安排说明
                        <textarea
                          v-model="editor.points[point.flatIndex].description"
                          rows="3"
                          maxlength="2000"
                          @input="markDirty"
                        ></textarea>
                      </label>
                    </article>
                  </template>

                  <div
                    v-if="isDropPlaceholder(null, group.day)"
                    class="route-point-placeholder"
                  >
                    <span>释放后插入到这里</span>
                  </div>
                </div>

                <div
                  v-else
                  class="route-day-empty"
                  :class="{
                    'is-drag-target': isDropPlaceholder(null, group.day),
                  }"
                >
                  <span v-if="isDropPlaceholder(null, group.day)"
                    >释放后插入到这里</span
                  >
                  <span v-else>这一天还没有点位，点击右上角可以补一个。</span>
                </div>
              </section>
            </div>
          </section>

          <div class="route-form-actions">
            <button
              v-if="!editor.routePlanId && !hasPoints"
              type="button"
              class="primary-button"
              :disabled="generating"
              @click="emit('generate-plan')"
            >
              {{ generating ? "生成中..." : "生成路线" }}
            </button>
            <button
              v-else-if="!editor.routePlanId && hasPoints"
              type="button"
              class="primary-button"
              :disabled="saving"
              @click="emit('save-plan')"
            >
              {{ saving ? "保存中..." : "保存路线" }}
            </button>
            <button
              v-else
              type="button"
              class="primary-button"
              :disabled="saving || !dirty"
              @click="emit('save-plan')"
            >
              {{ saving ? "保存中..." : dirty ? "保存修改" : "已保存" }}
            </button>
            <button
              type="button"
              class="primary-button route-export-button"
              @click="emit('export-plan')"
            >
              导出攻略
            </button>
          </div>
        </div>

        <div class="route-map-column">
          <section
            ref="routeMapCardRef"
            class="route-card route-map-card"
            :class="{ 'is-adding-point': pendingAddDay !== null }"
          >
            <div class="route-card-head">
              <div>
                <p>地图预览</p>
                <h3>点位与连线</h3>
              </div>
              <span v-if="pendingAddDay !== null" class="route-map-add-status">
                Day {{ pendingAddDay }}
              </span>
              <span v-else-if="pendingEditIndex !== null" class="route-map-add-status">
                编辑点位 {{ pendingEditIndex + 1 }}
              </span>
            </div>

            <RouteMap
              :points="sortedPoints"
              :map-config="mapConfig"
              :service-warning="mapServiceWarning"
              :editable="true"
              :is-adding-point="pendingAddDay !== null || pendingEditIndex !== null"
              @map-click="handleRouteMapClick"
              @point-click="handleRouteMapPointClick"
              @point-drag="handleRouteMapPointDrag"
            />
          </section>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";

import {
  addDaysToDate,
  calculateRouteDays,
  normalizeRouteDateRange,
} from "../utils/routePlanner";
import RouteMap from "./RouteMap.vue";

const props = defineProps({
  plans: {
    type: Array,
    default: () => [],
  },
  loadingPlans: {
    type: Boolean,
    default: false,
  },
  activePlanId: {
    type: [String, Number],
    default: null,
  },
  editor: {
    type: Object,
    required: true,
  },
  generating: {
    type: Boolean,
    default: false,
  },
  saving: {
    type: Boolean,
    default: false,
  },
  dirty: {
    type: Boolean,
    default: false,
  },
  isAuthenticated: {
    type: Boolean,
    default: false,
  },
  mapConfig: {
    type: Object,
    default: () => ({}),
  },
  mapServiceWarning: {
    type: String,
    default: "",
  },
});

const emit = defineEmits([
  "add-point",
  "back-chat",
  "create-draft",
  "delete-plan",
  "export-plan",
  "generate-plan",
  "mark-dirty",
  "move-point",
  "remove-point",
  "save-plan",
  "select-plan",
]);

const draggingIndex = ref(null);
const dragOverIndex = ref(null);
const dragOverDay = ref(null);
const dragSourceHidden = ref(false);
const dragRenderKey = ref(0);
const historyOpen = ref(false);
const dateRangeInputRef = ref(null);
const routeEditorScrollRef = ref(null);
const routeMapCardRef = ref(null);
const dateRangeInputText = ref("");
const pendingAddDay = ref(null);
const pendingEditIndex = ref(null);
let dateRangeScrollTimer = null;
let dateRangeScrollDirection = 1;
let dragGhostEl = null;
let autoScrollFrame = null;
let autoScrollSpeed = 0;
let lastDragClientY = null;
let dragCleanupTimer = null;
const inputScrollStates = new WeakMap();

const hasPoints = computed(() =>
  Array.isArray(props.editor.points) && props.editor.points.length > 0,
);

const sortedPoints = computed(() =>
  [...(props.editor.points || [])].map((point, flatIndex) => ({
    ...point,
    flatIndex,
  })).sort(
    (left, right) => left.day - right.day || left.order - right.order,
  ),
);

const dayGroups = computed(() => {
  const groups = Array.from(
    { length: Math.max(Number(props.editor.days) || 1, 1) },
    (_, index) => ({ day: index + 1, points: [] }),
  );

  [...(props.editor.points || [])]
    .map((point, flatIndex) => ({ ...point, flatIndex }))
    .sort((left, right) => left.day - right.day || left.order - right.order)
    .forEach((point) => {
      const dayIndex = Math.min(Math.max(point.day || 1, 1), groups.length) - 1;
      groups[dayIndex].points.push(point);
    });

  groups.forEach((group) => {
    group.points.forEach((point, index) => {
      point.order = index + 1;
    });
  });
  return groups;
});

function markDirty() {
  emit("mark-dirty");
}

function formatDateForInput(value) {
  return value ? String(value).replaceAll("-", "/") : "";
}

function formatDateRangeForInput(
  startDate = props.editor.startDate,
  endDate = props.editor.endDate,
) {
  const start = formatDateForInput(startDate);
  const end = formatDateForInput(endDate);
  return start && end ? `${start} - ${end}` : start || end || "";
}

function normalizeDateText(value) {
  const match = String(value || "")
    .trim()
    .match(/^(\d{4})[-\/年.](\d{1,2})[-\/月.](\d{1,2})日?$/);
  if (!match) {
    return "";
  }
  const [, year, month, day] = match;
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function parseDateRangeInput(value) {
  const dates = String(value || "")
    .match(/\d{4}[-\/年.]\d{1,2}[-\/月.]\d{1,2}日?/g)
    ?.map(normalizeDateText)
    .filter(Boolean);
  if (!dates || dates.length < 2) {
    return null;
  }
  return {
    startDate: dates[0],
    endDate: dates[1],
  };
}

function syncDateRangeInputText() {
  dateRangeInputText.value = formatDateRangeForInput();
}

function commitDateRangeInput() {
  const range = parseDateRangeInput(dateRangeInputText.value);
  if (!range) {
    syncDateRangeInputText();
    return;
  }
  applyDateRange(range.startDate, range.endDate);
}

function resetDateRangeInput() {
  stopDateRangeScroll();
  syncDateRangeInputText();
}

function stopDateRangeScroll() {
  if (dateRangeScrollTimer) {
    window.clearInterval(dateRangeScrollTimer);
    dateRangeScrollTimer = null;
  }
  dateRangeScrollDirection = 1;
  if (dateRangeInputRef.value) {
    dateRangeInputRef.value.scrollLeft = 0;
  }
}

function stopScrollableInput(event) {
  const input = event?.currentTarget;
  if (!input) {
    return;
  }
  const state = inputScrollStates.get(input);
  if (state?.timer) {
    window.clearInterval(state.timer);
  }
  inputScrollStates.delete(input);
  input.scrollLeft = 0;
}

function startScrollableInput(event) {
  const input = event?.currentTarget;
  if (!input || input.scrollWidth <= input.clientWidth + 1) {
    return;
  }
  stopScrollableInput(event);
  const state = { direction: 1, timer: null };
  state.timer = window.setInterval(() => {
    const maxScroll = input.scrollWidth - input.clientWidth;
    if (maxScroll <= 0) {
      stopScrollableInput(event);
      return;
    }
    input.scrollLeft += state.direction;
    if (input.scrollLeft >= maxScroll) {
      state.direction = -1;
    } else if (input.scrollLeft <= 0) {
      state.direction = 1;
    }
  }, 24);
  inputScrollStates.set(input, state);
}

function startDateRangeScroll() {
  const input = dateRangeInputRef.value;
  if (!input || input.scrollWidth <= input.clientWidth + 1) {
    return;
  }
  stopDateRangeScroll();
  dateRangeScrollTimer = window.setInterval(() => {
    const el = dateRangeInputRef.value;
    if (!el) {
      stopDateRangeScroll();
      return;
    }
    const maxScroll = el.scrollWidth - el.clientWidth;
    if (maxScroll <= 0) {
      stopDateRangeScroll();
      return;
    }
    el.scrollLeft += dateRangeScrollDirection;
    if (el.scrollLeft >= maxScroll) {
      dateRangeScrollDirection = -1;
    } else if (el.scrollLeft <= 0) {
      dateRangeScrollDirection = 1;
    }
  }, 24);
}

watch(
  () => [props.editor.startDate, props.editor.endDate],
  () => {
    syncDateRangeInputText();
    nextTick(() => {
      if (dateRangeInputRef.value) {
        dateRangeInputRef.value.scrollLeft = 0;
      }
    });
  },
  { immediate: true },
);

function applyDateRange(startDate, endDate) {
  const dateRange = normalizeRouteDateRange({
    ...props.editor,
    startDate,
    endDate,
  });
  props.editor.startDate = dateRange.startDate;
  props.editor.endDate = dateRange.endDate;
  props.editor.days = dateRange.days;
  markDirty();
}

function updateStartDate(value) {
  const days = Math.max(Number(props.editor.days) || 1, 1);
  applyDateRange(value, addDaysToDate(value, days - 1));
}

function updateEndDate(value) {
  applyDateRange(props.editor.startDate, value);
}

function dateForDay(day) {
  return addDaysToDate(
    props.editor.startDate,
    Math.max(Number(day) || 1, 1) - 1,
  );
}

function dateLabelForDay(day) {
  const date = dateForDay(day);
  return date ? `Day ${day} · ${date}` : `Day ${day}`;
}

function routeDateRangeLabel(route) {
  if (route?.startDate && route?.endDate) {
    return `${String(route.startDate).replaceAll("-", "/")} - ${String(route.endDate).replaceAll("-", "/")}`;
  }
  return `${route?.days || 1} 天`;
}

function toggleHistoryRoutes() {
  historyOpen.value = !historyOpen.value;
}

function closeHistoryRoutes() {
  historyOpen.value = false;
}

function selectPlanFromHistory(plan) {
  if (!plan) {
    return;
  }
  emit("select-plan", plan);
  historyOpen.value = false;
}

function createDraftFromHistory() {
  historyOpen.value = false;
  emit("create-draft");
}

function scrollToPoint(index) {
  nextTick(() => {
    const card = document.querySelector(`[data-route-point-index="${index}"]`);
    const input = card?.querySelector?.("input[data-route-point-name-input]");

    if (!card) {
      return;
    }

    card.scrollIntoView({ block: "center", behavior: "smooth" });
    input?.focus();
  });
}

function scrollToLastPoint(day) {
  nextTick(() => {
    const selector = `[data-route-point-day="${day}"] input[data-route-point-name-input]`;
    const inputs = Array.from(document.querySelectorAll(selector));
    const targetInput = inputs.at(-1);

    if (!targetInput) {
      return;
    }

    targetInput.scrollIntoView({ block: "center", behavior: "smooth" });
    targetInput.focus();
  });
}

function handleAddPoint(day) {
  const targetDay = Math.max(Number(day) || 1, 1);
  pendingAddDay.value = targetDay;
  pendingEditIndex.value = null;

  nextTick(() => {
    routeMapCardRef.value?.scrollIntoView({ block: "center", behavior: "smooth" });
  });
}

function handleEditPointLocation(index) {
  pendingEditIndex.value = index;
  pendingAddDay.value = null;

  nextTick(() => {
    routeMapCardRef.value?.scrollIntoView({ block: "center", behavior: "smooth" });
  });
}

function hasCoordinates(point) {
  return Number.isFinite(point?.latitude) && Number.isFinite(point?.longitude);
}

function handlePointCardClick(index) {
  const point = props.editor.points[index];
  if (!hasCoordinates(point) && pendingEditIndex.value !== index) {
    handleEditPointLocation(index);
  }
}

function handleRouteMapClick(payload) {
  if (pendingAddDay.value !== null) {
    const targetDay = pendingAddDay.value;
    pendingAddDay.value = null;
    emit("add-point", {
      day: targetDay,
      latitude: payload?.latitude,
      longitude: payload?.longitude,
    });
    scrollToLastPoint(targetDay);
    return;
  }

  if (pendingEditIndex.value !== null) {
    const index = pendingEditIndex.value;
    pendingEditIndex.value = null;
    props.editor.points[index].latitude = payload?.latitude;
    props.editor.points[index].longitude = payload?.longitude;
    markDirty();
    scrollToPoint(index);
    return;
  }
}

function handleRouteMapPointClick(payload) {
  pendingAddDay.value = null;
  pendingEditIndex.value = null;
  const index = Number(payload?.index);

  if (!Number.isInteger(index) || index < 0) {
    return;
  }

  scrollToPoint(index);
}

function handleRouteMapPointDrag(payload) {
  const index = Number(payload?.index);
  const latitude = Number(payload?.latitude);
  const longitude = Number(payload?.longitude);

  if (
    !Number.isInteger(index) ||
    index < 0 ||
    index >= props.editor.points.length ||
    !Number.isFinite(latitude) ||
    !Number.isFinite(longitude)
  ) {
    return;
  }

  pendingAddDay.value = null;
  pendingEditIndex.value = null;
  props.editor.points[index].latitude = latitude;
  props.editor.points[index].longitude = longitude;
  markDirty();
}

function handleDocumentPointerDown(event) {
  if (pendingAddDay.value === null && pendingEditIndex.value === null) {
    return;
  }

  const target = event.target;

  const mapShell = routeMapCardRef.value?.querySelector?.(".route-map-shell");

  if (target instanceof Node && mapShell?.contains?.(target)) {
    return;
  }

  pendingAddDay.value = null;
  pendingEditIndex.value = null;
}

function addTip() {
  props.editor.tips.push("");
  markDirty();
}

function removeTip(index) {
  props.editor.tips.splice(index, 1);
  markDirty();
}

function clearDragState() {
  if (dragCleanupTimer) {
    window.clearTimeout(dragCleanupTimer);
    dragCleanupTimer = null;
  }

  const wasDragging = draggingIndex.value != null;
  draggingIndex.value = null;
  dragOverIndex.value = null;
  dragOverDay.value = null;
  dragSourceHidden.value = false;
  lastDragClientY = null;
  stopDragAutoScroll();

  if (dragGhostEl) {
    dragGhostEl.remove();
    dragGhostEl = null;
  }

  // 原生拖拽结束后，某些浏览器会把 DOM 重绘延迟到下一次交互。
  // 这里主动刷新 route-days 的 key，避免”释放后插入到这里”残留到下一次点击。
  // 仅在实际发生拖拽时才刷新 key，避免干扰正常的点击事件处理。
  if (wasDragging) {
    dragRenderKey.value += 1;
    nextTick(() => {
      requestAnimationFrame(() => {
        dragRenderKey.value += 1;
      });
    });
  }
}

function scheduleClearDragState(delay = 0) {
  if (dragCleanupTimer) {
    window.clearTimeout(dragCleanupTimer);
  }

  dragCleanupTimer = window.setTimeout(() => {
    clearDragState();
  }, delay);
}

function setDragTarget(targetIndex, targetDay) {
  if (draggingIndex.value == null) {
    return;
  }
  dragOverIndex.value = targetIndex;
  dragOverDay.value = targetDay;
}

function isDropPlaceholder(targetIndex, targetDay) {
  return (
    draggingIndex.value != null &&
    dragOverDay.value === targetDay &&
    dragOverIndex.value === targetIndex &&
    !(
      targetIndex === draggingIndex.value &&
      targetDay === props.editor.points?.[draggingIndex.value]?.day
    )
  );
}

function startDrag(index, event) {
  draggingIndex.value = index;
  dragOverIndex.value = index;
  dragOverDay.value = props.editor.points?.[index]?.day || null;
  dragSourceHidden.value = false;
  lastDragClientY = event?.clientY ?? null;

  event?.dataTransfer?.setData("text/plain", String(index));

  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = "move";
  }

  const sourceCard = event?.currentTarget?.closest?.(".route-point-card");

  if (sourceCard && event?.dataTransfer) {
    dragGhostEl = sourceCard.cloneNode(true);
    dragGhostEl.classList.add("route-point-drag-ghost");
    dragGhostEl.style.width = `${sourceCard.offsetWidth}px`;
    dragGhostEl.style.position = "fixed";
    dragGhostEl.style.top = "-9999px";
    dragGhostEl.style.left = "-9999px";
    dragGhostEl.style.pointerEvents = "none";

    document.body.appendChild(dragGhostEl);
    event.dataTransfer.setDragImage(dragGhostEl, 24, 24);
  }

  requestAnimationFrame(() => {
    if (draggingIndex.value === index) {
      dragSourceHidden.value = true;
    }
  });
}

function handleEditorDragOver(event) {
  if (draggingIndex.value == null) {
    return;
  }
  updateDragAutoScroll(event);
}

function handleEditorDrop() {
  clearDragState();
}

function handleEditorDragLeave(event) {
  if (draggingIndex.value == null) {
    return;
  }

  const container = routeEditorScrollRef.value;
  const relatedTarget = event.relatedTarget;

  if (
    container &&
    relatedTarget instanceof Node &&
    container.contains(relatedTarget)
  ) {
    return;
  }

  scheduleClearDragState();
}

function handlePointDragOver(targetIndex, targetDay, event) {
  setDragTarget(targetIndex, targetDay);
  updateDragAutoScroll(event);
}

function handleDayDragOver(targetDay, event) {
  setDragTarget(null, targetDay);
  updateDragAutoScroll(event);
}

function getRouteScrollContainer() {
  const container = routeEditorScrollRef.value;

  if (container) {
    return container;
  }

  return document.scrollingElement || document.documentElement;
}

function canScrollElement(element, direction) {
  if (!element) {
    return false;
  }

  if (direction < 0) {
    return element.scrollTop > 0;
  }

  return element.scrollTop + element.clientHeight < element.scrollHeight - 1;
}

function updateDragAutoScroll(event) {
  if (draggingIndex.value == null || !event) {
    stopDragAutoScroll();
    return;
  }

  lastDragClientY = event.clientY;

  const container = getRouteScrollContainer();

  if (!container) {
    stopDragAutoScroll();
    return;
  }

  const isDocument =
    container === document.scrollingElement ||
    container === document.documentElement ||
    container === document.body;

  const rect = isDocument
    ? {
        top: 0,
        bottom: window.innerHeight,
      }
    : container.getBoundingClientRect();

  const edgeSize = 120;
  const maxSpeed = 22;

  const distanceToTop = lastDragClientY - rect.top;
  const distanceToBottom = rect.bottom - lastDragClientY;

  if (distanceToTop < edgeSize) {
    const ratio = Math.max(
      0,
      Math.min(1, (edgeSize - distanceToTop) / edgeSize),
    );
    autoScrollSpeed = -Math.max(4, Math.ceil(ratio * maxSpeed));
    startDragAutoScroll();
    return;
  }

  if (distanceToBottom < edgeSize) {
    const ratio = Math.max(
      0,
      Math.min(1, (edgeSize - distanceToBottom) / edgeSize),
    );
    autoScrollSpeed = Math.max(4, Math.ceil(ratio * maxSpeed));
    startDragAutoScroll();
    return;
  }

  stopDragAutoScroll();
}

function startDragAutoScroll() {
  if (autoScrollFrame) {
    return;
  }

  const step = () => {
    const container = getRouteScrollContainer();

    if (!container || draggingIndex.value == null || autoScrollSpeed === 0) {
      autoScrollFrame = null;
      return;
    }

    const direction = autoScrollSpeed > 0 ? 1 : -1;

    if (canScrollElement(container, direction)) {
      container.scrollTop += autoScrollSpeed;
    }

    autoScrollFrame = requestAnimationFrame(step);
  };

  autoScrollFrame = requestAnimationFrame(step);
}

function stopDragAutoScroll() {
  autoScrollSpeed = 0;

  if (autoScrollFrame) {
    cancelAnimationFrame(autoScrollFrame);
    autoScrollFrame = null;
  }
}

function dropOnPoint(targetIndex, targetDay) {
  if (draggingIndex.value == null) {
    clearDragState();
    return;
  }

  emit("move-point", {
    from: draggingIndex.value,
    to: targetIndex,
    day: targetDay,
  });

  clearDragState();
}

function endDrag() {
  clearDragState();
}

function dropIntoDay(day) {
  if (draggingIndex.value == null) {
    clearDragState();
    return;
  }

  emit("move-point", {
    from: draggingIndex.value,
    to: dragOverDay.value === day ? dragOverIndex.value : null,
    day,
  });

  clearDragState();
}

function handleDocumentDrop() {
  scheduleClearDragState(0);
}

onMounted(() => {
  document.addEventListener("pointerdown", handleDocumentPointerDown, true);
  document.addEventListener("click", closeHistoryRoutes);
  document.addEventListener("dragend", clearDragState, true);
  document.addEventListener("drop", handleDocumentDrop, true);
  document.addEventListener("mouseup", clearDragState, true);
  document.addEventListener("pointerup", clearDragState, true);
  window.addEventListener("blur", clearDragState);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointerDown, true);
  document.removeEventListener("click", closeHistoryRoutes);
  document.removeEventListener("dragend", clearDragState, true);
  document.removeEventListener("drop", handleDocumentDrop, true);
  document.removeEventListener("mouseup", clearDragState, true);
  document.removeEventListener("pointerup", clearDragState, true);
  window.removeEventListener("blur", clearDragState);

  stopDateRangeScroll();
  stopDragAutoScroll();
  clearDragState();
});
</script>

<style scoped>
.route-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  min-height: 0;
  flex: 1;
  padding: 18px 22px 22px;
  overflow: hidden;
}

.route-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.94) 0%,
    rgba(248, 250, 252, 0.98) 100%
  );
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.route-history-head,
.route-card-head,
.route-day-head,
.route-point-card-head,
.route-inline-actions,
.route-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.route-history-head p,
.route-card-head p {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.route-history-head h2,
.route-card-head h3 {
  margin: 0;
  letter-spacing: -0.03em;
}

.route-history-head h2 {
  font-size: 26px;
}

.route-library-card {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.route-library-card.active {
  border-color: rgba(16, 163, 127, 0.24);
  box-shadow: inset 0 0 0 1px rgba(16, 163, 127, 0.14);
}

.route-library-card-main {
  display: grid;
  gap: 6px;
  width: 100%;
  color: #0f172a;
  text-align: left;
  cursor: pointer;
}

.route-library-card-main strong {
  font-size: 15px;
  font-weight: 700;
}

.route-library-card-main span,
.route-library-card-main small,
.route-library-empty span {
  color: #475569;
  line-height: 1.6;
}

.route-library-card-delete {
  justify-self: flex-start;
  color: #b91c1c;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.route-library-empty {
  display: grid;
  gap: 6px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.03);
}

.route-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.route-detail-head {
  position: relative;
  z-index: 1200;
  justify-content: flex-end;
  flex: none;
  min-height: 44px;
  padding: 0 2px;
}

.route-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(0, 5fr);
  grid-template-rows: minmax(0, 1fr);
  gap: 18px;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.route-editor-column,
.route-map-column {
  display: grid;
  gap: 18px;
  min-height: 0;
}

.route-editor-column {
  align-content: start;
  height: 100%;
  max-height: 100%;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
}

.route-map-column {
  grid-template-rows: minmax(0, 1fr);
}

.route-map-card {
  min-height: 0;
  grid-template-rows: auto minmax(0, 1fr);
}

.route-map-card.is-adding-point {
  border-color: rgba(16, 163, 127, 0.34);
  box-shadow:
    inset 0 0 0 1px rgba(16, 163, 127, 0.16),
    0 18px 36px rgba(15, 23, 42, 0.08);
}

.route-map-card :deep(.route-map-shell) {
  min-height: 0;
  height: 100%;
}

.route-map-add-status {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 6px 11px;
  border-radius: 999px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  background: rgba(16, 163, 127, 0.12);
}

.route-card {
  display: grid;
  gap: 18px;
  padding: 20px;
  border-radius: 26px;
}

.route-history-menu {
  position: relative;
  z-index: 1300;
}

.route-history-trigger {
  min-width: 112px;
}

.route-history-popover {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  z-index: 1300;
  display: grid;
  gap: 14px;
  width: min(380px, calc(100vw - 44px));
  max-height: min(620px, calc(100vh - 140px));
  overflow-y: auto;
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 26px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 250, 252, 0.98) 100%
  );
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.16);
}

.route-history-content {
  display: grid;
  gap: 10px;
  max-height: min(430px, calc(100vh - 260px));
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.route-form-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 12px;
  padding-bottom: 2px;
}

.route-form-actions .primary-button {
  width: 100%;
  min-width: 0;
}

.route-export-button {
  background: linear-gradient(135deg, #475569 0%, #334155 100%);
}

.route-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.route-date-range-field {
  grid-column: auto;
  min-width: 0;
}

.route-date-range-field input {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: clip;
}

.route-point-grid {
  grid-template-columns: minmax(0, 1fr);
}

.route-point-drag-handle {
  cursor: grab;
  user-select: none;
}

.route-point-drag-handle:active {
  cursor: grabbing;
}

.route-stack-field {
  display: grid;
  gap: 8px;
}

.route-stack-field textarea {
  min-height: 98px;
}

.route-tips,
.route-days,
.route-point-list {
  display: grid;
  gap: 14px;
}

.route-tip-item {
  display: grid;
  gap: 8px;
}

.route-tip-item span,
.route-day-head span {
  color: #64748b;
  font-size: 13px;
}

.route-inline-actions {
  align-items: stretch;
}

.route-inline-actions .ghost-button {
  flex: none;
}

.route-day-section {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 22px;
  background: rgba(15, 23, 42, 0.035);
  border: 1px dashed rgba(15, 23, 42, 0.08);
}

.route-day-head strong {
  display: block;
  margin-bottom: 4px;
  font-size: 18px;
}

.route-day-head .ghost-button.active {
  color: #0f766e;
  background: rgba(16, 163, 127, 0.12);
  border-color: rgba(16, 163, 127, 0.22);
}

.route-point-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  cursor: default;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease,
    opacity 0.18s ease;
}

.route-point-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.1);
}

.route-point-card.no-coordinates {
  border-color: rgba(251, 146, 60, 0.24);
  background: rgba(255, 247, 237, 0.92);
  cursor: pointer;
}

.route-point-card.no-coordinates:hover {
  border-color: rgba(251, 146, 60, 0.38);
  box-shadow: 0 14px 30px rgba(251, 146, 60, 0.16);
}

.route-point-card.is-pending-edit {
  border-color: rgba(16, 163, 127, 0.34);
  background: rgba(240, 253, 250, 0.92);
  box-shadow: inset 0 0 0 1px rgba(16, 163, 127, 0.16);
}

.route-point-card:active {
  cursor: grabbing;
}

.route-point-card.is-dragging-source {
  height: 0;
  min-height: 0;
  margin: 0;
  padding-top: 0;
  padding-bottom: 0;
  border-width: 0;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
  transform: none;
}

.route-point-placeholder {
  display: grid;
  place-items: center;
  min-height: 118px;
  border-radius: 20px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: repeating-linear-gradient(
    -45deg,
    rgba(16, 163, 127, 0.08),
    rgba(16, 163, 127, 0.08) 10px,
    rgba(16, 163, 127, 0.14) 10px,
    rgba(16, 163, 127, 0.14) 20px
  );
  border: 1px dashed rgba(15, 118, 110, 0.38);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.62),
    0 12px 26px rgba(15, 118, 110, 0.08);
  animation: routeDropPlaceholderIn 0.16s ease-out both;
}

.route-point-placeholder span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
}

.route-point-drag-ghost {
  position: fixed;
  top: -1000px;
  left: -1000px;
  z-index: -1;
  pointer-events: none;
  opacity: 0.88;
  transform: rotate(1deg) scale(0.98);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.22);
}

@keyframes routeDropPlaceholderIn {
  from {
    min-height: 0;
    opacity: 0;
    transform: scaleY(0.86);
  }
  to {
    min-height: 118px;
    opacity: 1;
    transform: scaleY(1);
  }
}

.route-point-card-head strong {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.route-point-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  background: rgba(16, 163, 127, 0.12);
}

.route-day-empty {
  padding: 14px 16px;
  border-radius: 18px;
  color: #64748b;
  background: rgba(255, 255, 255, 0.72);
}

.route-day-empty.is-drag-target {
  display: grid;
  place-items: center;
  min-height: 92px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 700;
  background: rgba(16, 163, 127, 0.08);
  border: 1px dashed rgba(15, 118, 110, 0.38);
}

.compact {
  padding: 9px 14px;
  border-radius: 14px;
  font-size: 13px;
}

.danger {
  color: #b91c1c;
}

@media (max-width: 1120px) {
  .route-workspace {
    overflow-y: auto;
  }

  .route-detail {
    overflow: visible;
  }

  .route-detail-grid {
    grid-template-columns: minmax(0, 1fr);
    grid-template-rows: auto auto;
    flex: none;
    overflow: visible;
  }

  .route-editor-column {
    height: auto;
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }

  .route-map-column {
    grid-template-rows: auto;
  }

  .route-map-card :deep(.route-map-shell) {
    min-height: 420px;
    height: 420px;
  }
}

@media (max-width: 959px) {
  .route-workspace {
    grid-template-columns: minmax(0, 1fr);
    padding: 14px;
  }

  .route-history-head,
  .route-card-head,
  .route-day-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .route-detail-head {
    align-items: flex-end;
  }

  .route-form-grid,
  .route-point-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .route-form-actions {
    justify-content: stretch;
  }

  .route-form-actions .primary-button {
    width: 100%;
  }
}
</style>
