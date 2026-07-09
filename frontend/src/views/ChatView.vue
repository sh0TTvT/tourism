<template>
  <ChatWorkspace
    ref="chatWorkspaceRef"
    :chat-input="chatInput"
    :is-mobile-viewport="isMobileViewport"
    :is-model-menu-open="isModelMenuOpen"
    :models="models"
    :available-models="availableModels"
    :unavailable-models="unavailableModels"
    :provider="provider"
    :model="model"
    :default-provider="defaultProvider"
    :default-model="defaultModel"
    :messages="messages"
    :starter-prompts="starterPrompts"
    :user-display-name="userDisplayName"
    :composer-disabled="composerDisabled"
    :send-button-disabled="sendButtonDisabled"
    :is-streaming="sendingChat"
    :composer-placeholder="composerPlaceholder"
    :route-generating="routeGenerating"
    :weather-service-warning="weatherServiceWarning"
    :weather-forecast="weatherForecast"
    :user-location="userLocation"
    :locating-user="locatingUser"
    :loading-weather="loadingWeather"
    :weather-error="weatherError"
    :is-weather-panel-open="isWeatherPanelOpen"
    @update:chat-input="$emit('update:chatInput', $event)"
    @open-mobile-sidebar="$emit('openMobileSidebar')"
    @open-route-draft="$emit('openRouteDraft', $event)"
    @select-model-option="$emit('selectModelOption', $event)"
    @send-message="$emit('sendMessage')"
    @stop-streaming="$emit('stopStreaming')"
    @copy-message="$emit('copyMessage', $event)"
    @regenerate-message="$emit('regenerateMessage', $event)"
    @toggle-model-menu="$emit('toggleModelMenu')"
    @toggle-weather-panel="$emit('toggleWeatherPanel')"
    @close-weather-panel="$emit('closeWeatherPanel')"
    @refresh-weather="$emit('refreshWeather')"
    @use-starter-prompt="$emit('useStarterPrompt', $event)"
  />
</template>

<script setup>
import { ref } from "vue";
import ChatWorkspace from "../components/ChatWorkspace.vue";

const chatWorkspaceRef = ref(null);

defineProps({
  chatInput: { type: String, default: "" },
  isMobileViewport: { type: Boolean, default: false },
  isModelMenuOpen: { type: Boolean, default: false },
  models: { type: Array, default: () => [] },
  availableModels: { type: Array, default: () => [] },
  unavailableModels: { type: Array, default: () => [] },
  provider: { type: String, default: "" },
  model: { type: String, default: "" },
  defaultProvider: { type: String, default: "" },
  defaultModel: { type: String, default: "" },
  messages: { type: Array, default: () => [] },
  starterPrompts: { type: Array, default: () => [] },
  userDisplayName: { type: String, default: "" },
  composerDisabled: { type: Boolean, default: false },
  sendButtonDisabled: { type: Boolean, default: false },
  sendingChat: { type: Boolean, default: false },
  composerPlaceholder: { type: String, default: "" },
  routeGenerating: { type: Boolean, default: false },
  weatherServiceWarning: { type: String, default: "" },
  weatherForecast: { type: Object, default: null },
  userLocation: { type: Object, default: null },
  locatingUser: { type: Boolean, default: false },
  loadingWeather: { type: Boolean, default: false },
  weatherError: { type: String, default: "" },
  isWeatherPanelOpen: { type: Boolean, default: false },
});

defineEmits([
  "update:chatInput",
  "openMobileSidebar",
  "openRouteDraft",
  "selectModelOption",
  "sendMessage",
  "stopStreaming",
  "toggleModelMenu",
  "toggleWeatherPanel",
  "closeWeatherPanel",
  "refreshWeather",
  "useStarterPrompt",
]);

defineExpose({
  focusComposer: () => chatWorkspaceRef.value?.focusComposer?.(),
  scrollToBottom: () => chatWorkspaceRef.value?.scrollToBottom?.(),
  scheduleScrollToBottom: () => {
    if (typeof chatWorkspaceRef.value?.scheduleScrollToBottom === "function") {
      chatWorkspaceRef.value.scheduleScrollToBottom();
      return;
    }
    chatWorkspaceRef.value?.scrollToBottom?.();
  },
});
</script>
