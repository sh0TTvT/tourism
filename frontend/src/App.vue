<template>
  <div
    class="app-shell"
    :class="{
      'sidebar-collapsed': isSidebarCollapsed && !isMobileViewport,
      'sidebar-mobile-open': isMobileSidebarOpen,
      'mobile-layout': isMobileViewport,
    }"
  >
    <div
      v-if="isMobileViewport && isMobileSidebarOpen"
      class="sidebar-backdrop"
      @click="closeMobileSidebar"
    ></div>

    <AppSidebar
      ref="sidebarRef"
      v-model:history-query="historyQuery"
      :current-view="currentView"
      :explore-tab="exploreTab"
      :explore-mine-view="exploreMineView"
      :favorite-count="favoriteExplorePosts.length"
      :is-search-open="isSearchOpen"
      :is-sidebar-collapsed="isSidebarCollapsed"
      :loading-conversations="loadingConversations"
      :is-authenticated="isAuthenticated"
      :filtered-conversations="filteredConversations"
      :selected-conversation-id="selectedConversationId"
      :deleting-conversation-id="deletingConversationId"
      :messages-length="messages.length"
      :is-account-menu-open="isAccountMenuOpen"
      :is-account-modal-open="isAccountModalOpen"
      :user-initial="userInitial"
      :user-display-name="userDisplayName"
      :user-email-label="userEmailLabel"
      :sidebar-toggle-tooltip="sidebarToggleTooltip"
      :sidebar-toggle-icon="sidebarToggleIcon"
      @delete-conversation="deleteConversation"
      @leave-explore="leaveExplore"
      @logout="logout"
      @open-account-modal="openAccountModal"
      @open-explore-view="openExploreView"
      @open-routes-view="openRoutesView"
      @select-conversation="selectConversation"
      @set-explore-tab="setExploreTab"
      @start-new-chat="startNewChat"
      @switch-view="switchView"
      @toggle-account-menu="toggleAccountMenu"
      @toggle-search="toggleSearch"
      @toggle-sidebar="toggleSidebar"
    />

    <main class="app-main">
      <ChatView
        v-if="currentView === 'chat'"
        ref="chatWorkspaceRef"
        v-model:chat-input="chatInput"
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
        :sending-chat="sendingChat"
        :composer-placeholder="composerPlaceholder"
        :route-generating="routeGenerating"
        :weather-service-warning="weatherServiceWarning"
        :weather-forecast="weatherForecast"
        :user-location="userLocation"
        :locating-user="locatingUser"
        :loading-weather="loadingWeather"
        :weather-error="weatherError"
        :is-weather-panel-open="isWeatherPanelOpen"
        @open-mobile-sidebar="openMobileSidebar"
        @open-route-draft="openRouteDraftFromMessage"
        @select-model-option="selectModelOption"
        @send-message="sendMessage"
        @stop-streaming="stopStreamingResponse"
        @toggle-model-menu="toggleModelMenu"
        @toggle-weather-panel="toggleWeatherPanel"
        @close-weather-panel="closeWeatherPanel"
        @refresh-weather="refreshWeather"
        @use-starter-prompt="useStarterPrompt"
      />

      <RoutesView
        v-else-if="currentView === 'routes'"
        :is-mobile-viewport="isMobileViewport"
        :plans="routePlans"
        :loading-plans="loadingRoutePlans || loadingRouteDetail"
        :active-plan-id="activeRoutePlanId"
        :editor="routeEditor"
        :generating="routeGenerating"
        :saving="routeSaving"
        :dirty="routeDirty"
        :is-authenticated="isAuthenticated"
        :map-config="mapRuntimeConfig"
        :map-service-warning="mapServiceWarning"
        @open-mobile-sidebar="openMobileSidebar"
        @back-chat="switchView('chat')"
        @create-draft="createRouteDraft"
        @select-plan="selectRoutePlan"
        @delete-plan="deleteRoutePlan"
        @generate-plan="generateRoutePlan"
        @save-plan="saveRoutePlan"
        @export-plan="exportRoutePlan"
        @add-point="addRoutePoint"
        @remove-point="removeRoutePoint"
        @move-point="moveRoutePoint"
        @mark-dirty="markRouteDirty"
      />

      <ExploreView
        v-else-if="currentView === 'explore'"
        :tab="exploreTab"
        :mine-view="exploreMineView"
        :mine-category="exploreMineCategory"
        :loading="loadingExplorePosts"
        :loading-route-plans="loadingRoutePlans"
        :loading-route-plan-detail="loadingExploreRoutePlanDetail"
        :posts="exploreFeedPosts"
        :own-posts="ownExplorePosts"
        :favorite-posts="favoriteExplorePosts"
        :route-plans="routePlans"
        :selected-route-plan-detail="exploreRoutePlanDetail"
        :is-authenticated="isAuthenticated"
        :is-admin="isAdmin"
        :is-mobile-viewport="isMobileViewport"
        :published-marker="explorePublishedMarker"
        :last-published-post-id="lastPublishedExplorePostId"
        :navigation-marker="exploreNavigationMarker"
        :sharing-route-plan-id="sharingRoutePlanId"
        :map-config="mapRuntimeConfig"
        :map-service-warning="mapServiceWarning"
        :request-confirm="requestConfirm"
        @add-comment="addExploreComment"
        @delete-comment="deleteExploreComment"
        @delete-post="deleteExplorePost"
        @delete-route="deleteRoutePlan"
        @import-route="importSharedRoute"
        @notify="showNotice($event.message, $event.type)"
        @open-mobile-sidebar="openMobileSidebar"
        @publish-post="publishExplorePost"
        @record-route-click="recordExploreRouteClick"
        @require-auth="openAccountModal('account')"
        @select-route-plan-detail="openExploreRoutePlanDetail"
        @set-mine-category="setExploreMineCategory"
        @set-mine-view="setExploreMineView"
        @share-route="shareRoutePlanToExplore"
        @start-point-chat="startPointChat"
        @toggle-comment-like="toggleExploreCommentLike"
        @toggle-favorite="toggleExploreFavorite"
        @toggle-like="toggleExploreLike"
      />
    </main>

    <transition name="fade-slide">
      <AccountModal
        v-if="isAccountModalOpen"
        :is-authenticated="isAuthenticated"
        :section="accountModalSection"
        :account-section-meta="accountSectionMeta"
        :user-initial="userInitial"
        :user-display-name="userDisplayName"
        :user-email-label="userEmailLabel"
        :user="user"
        :memory-label="memoryLabel"
        :profile-form="profileForm"
        :preference-form="preferenceForm"
        :password-form="passwordForm"
        :auth-tab="authTab"
        :auth-loading="authLoading"
        :auth-error="authError"
        :login-form="loginForm"
        :register-form="registerForm"
        :budget-options="budgetOptions"
        :memory-options="memoryOptions"
        :saving-profile="savingProfile"
        :saving-preferences="savingPreferences"
        :changing-password="changingPassword"
        @change-password="changePassword"
        @close="closeAccountModal"
        @login="login"
        @register="register"
        @save-preferences="savePreferences"
        @save-profile="saveProfile"
        @set-auth-tab="setAuthTab"
        @set-section="accountModalSection = $event"
      />
    </transition>

    <transition name="fade-slide">
      <div
        v-if="confirmDialog"
        class="confirm-dialog-backdrop"
        @click.self="closeConfirmDialog(false)"
      >
        <section
          class="confirm-dialog"
          role="dialog"
          aria-modal="true"
          :aria-label="confirmDialog.title"
        >
          <button
            type="button"
            class="confirm-dialog-close"
            aria-label="关闭确认弹窗"
            @click="closeConfirmDialog(false)"
          >
            <AppIcon name="close" />
          </button>

          <div class="confirm-dialog-copy">
            <p>{{ confirmDialog.eyebrow || "删除确认" }}</p>
            <h2>{{ confirmDialog.title }}</h2>
            <span>{{ confirmDialog.message }}</span>
          </div>

          <div class="confirm-dialog-actions">
            <button
              type="button"
              class="ghost-button"
              @click="closeConfirmDialog(false)"
            >
              取消
            </button>
            <button
              type="button"
              class="primary-button danger"
              @click="closeConfirmDialog(true)"
            >
              {{ confirmDialog.confirmLabel || "确认删除" }}
            </button>
          </div>
        </section>
      </div>
    </transition>

    <transition name="toast">
      <div v-if="notice" class="app-toast" :class="notice.type">
        {{ notice.message }}
      </div>
    </transition>
  </div>
</template>


<script setup>
import AccountModal from "./components/AccountModal.vue";
import AppIcon from "./components/AppIcon.vue";
import AppSidebar from "./components/AppSidebar.vue";
import ChatView from "./views/ChatView.vue";
import ExploreView from "./views/ExploreView.vue";
import RoutesView from "./views/RoutesView.vue";
import { useUserApp } from "./composables/useUserApp";

const {
  budgetOptions,
  memoryOptions,
  starterPrompts,
  token,
  user,
  currentView,
  exploreTab,
  exploreMineView,
  exploreMineCategory,
  previousPrimaryView,
  isSidebarCollapsed,
  isMobileViewport,
  isMobileSidebarOpen,
  isSearchOpen,
  historyQuery,
  isModelMenuOpen,
  isAccountMenuOpen,
  isAccountModalOpen,
  accountModalSection,
  provider,
  model,
  defaultProvider,
  defaultModel,
  models,
  conversations,
  selectedConversationId,
  messages,
  chatInput,
  loadingConversations,
  sendingChat,
  deletingConversationId,
  profileForm,
  preferenceForm,
  passwordForm,
  routePlans,
  loadingRoutePlans,
  loadingRouteDetail,
  routeGenerating,
  routeSaving,
  loadingExplorePosts,
  loadingExploreRoutePlanDetail,
  sharingRoutePlanId,
  activeRoutePlanId,
  routeEditor,
  routeSnapshot,
  explorePosts,
  exploreRoutePlanDetail,
  explorePublishedMarker,
  lastPublishedExplorePostId,
  exploreNavigationMarker,
  publicServices,
  mapRuntimeConfig,
  userLocation,
  weatherForecast,
  locatingUser,
  loadingWeather,
  weatherError,
  isWeatherPanelOpen,
  authTab,
  authLoading,
  authError,
  loginForm,
  registerForm,
  savingProfile,
  savingPreferences,
  changingPassword,
  notice,
  confirmDialog,
  sidebarRef,
  chatWorkspaceRef,
  isAuthenticated,
  selectedModel,
  sortedModels,
  availableModels,
  unavailableModels,
  publicServiceStatusMap,
  weatherServiceWarning,
  mapServiceWarning,
  modelSelectionKey,
  filteredConversations,
  routeDirty,
  exploreFeedPosts,
  ownExplorePosts,
  favoriteExplorePosts,
  isAdmin,
  userDisplayName,
  userEmailLabel,
  userInitial,
  memoryLabel,
  composerDisabled,
  sendButtonDisabled,
  composerPlaceholder,
  sidebarToggleTooltip,
  sidebarToggleIcon,
  accountSectionMeta,
  showNotice,
  requestConfirm,
  closeConfirmDialog,
  syncUserForms,
  applyRouteResponse,
  resetRouteEditor,
  confirmDiscardRouteChanges,
  syncViewport,
  syncHashView,
  switchView,
  toggleSidebar,
  openMobileSidebar,
  closeMobileSidebar,
  toggleModelMenu,
  closeModelMenu,
  toggleAccountMenu,
  closeAccountMenu,
  openAccountModal,
  closeAccountModal,
  finishAuthSuccess,
  selectModelOption,
  toggleSearch,
  resetChatInput,
  createChatMessage,
  mapConversationMessages,
  sleep,
  recoverInterruptedChatStream,
  startNewChat,
  useStarterPrompt,
  startFreshChatWithPrompt,
  loadModels,
  refreshWeather,
  toggleWeatherPanel,
  closeWeatherPanel,
  loadPublicServiceStatus,
  handleDocumentPointerDown,
  handleWindowKeydown,
  loadProfile,
  loadConversations,
  loadRoutePlans,
  loadExplorePosts,
  openRoutesView,
  openExploreView,
  setExploreTab,
  setExploreMineView,
  setExploreMineCategory,
  leaveExplore,
  selectConversation,
  deleteConversation,
  createRouteDraft,
  openRouteDraftFromMessage,
  selectRoutePlan,
  deleteRoutePlan,
  buildHistoryPayload,
  markRouteDirty,
  generateRoutePlan,
  saveRoutePlan,
  exportRoutePlan,
  addRoutePoint,
  removeRoutePoint,
  moveRoutePoint,
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
  sendMessage,
  stopStreamingResponse,
  saveProfile,
  savePreferences,
  changePassword,
  setAuthTab,
  login,
  register,
  clearSession,
  logout,
  shouldLoadExploreRoutePlans,
  shouldKeepExploreRouteDetail
} = useUserApp();
</script>
