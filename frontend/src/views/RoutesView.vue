<template>
  <header class="chat-toolbar route-toolbar">
    <div class="toolbar-left route-toolbar-left">
      <button
        v-if="isMobileViewport"
        type="button"
        class="icon-button mobile-nav-button"
        data-tooltip="打开边栏"
        title="打开边栏"
        aria-label="打开边栏"
        @click="$emit('openMobileSidebar')"
      >
        <AppIcon name="panel-open" />
      </button>

      <div class="route-toolbar-copy">
        <p>路线规划</p>
        <strong>从聊天生成、编辑并保存你的旅行路线</strong>
      </div>
    </div>
  </header>

  <RoutePlannerView
    :plans="plans"
    :loading-plans="loadingPlans"
    :active-plan-id="activePlanId"
    :editor="editor"
    :generating="generating"
    :saving="saving"
    :dirty="dirty"
    :is-authenticated="isAuthenticated"
    :map-config="mapConfig"
    :map-service-warning="mapServiceWarning"
    @back-chat="$emit('backChat')"
    @create-draft="$emit('createDraft')"
    @select-plan="$emit('selectPlan', $event)"
    @delete-plan="$emit('deletePlan', $event)"
    @generate-plan="$emit('generatePlan')"
    @save-plan="$emit('savePlan')"
    @export-plan="$emit('exportPlan')"
    @add-point="$emit('addPoint', $event)"
    @remove-point="$emit('removePoint', $event)"
    @move-point="$emit('movePoint', $event)"
    @mark-dirty="$emit('markDirty')"
  />
</template>

<script setup>
import AppIcon from "../components/AppIcon.vue";
import RoutePlannerView from "../components/RoutePlannerView.vue";

defineProps({
  isMobileViewport: { type: Boolean, default: false },
  plans: { type: Array, default: () => [] },
  loadingPlans: { type: Boolean, default: false },
  activePlanId: { type: [String, Number], default: null },
  editor: { type: Object, required: true },
  generating: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  dirty: { type: Boolean, default: false },
  isAuthenticated: { type: Boolean, default: false },
  mapConfig: { type: Object, default: () => ({}) },
  mapServiceWarning: { type: String, default: "" },
});

defineEmits([
  "openMobileSidebar",
  "backChat",
  "createDraft",
  "selectPlan",
  "deletePlan",
  "generatePlan",
  "savePlan",
  "exportPlan",
  "addPoint",
  "removePoint",
  "movePoint",
  "markDirty",
]);
</script>
