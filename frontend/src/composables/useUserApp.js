import { onBeforeUnmount, onMounted, watch } from "vue";

import {
  STORAGE_KEYS,
  budgetOptions,
  memoryOptions,
  starterPrompts,
} from "../constants/uiOptions";
import { addDaysToDate, clamp, normalizeRoutePoints, normalizeRouteDateRange } from "../utils/routePlanner";
import {
  setApiAuthTokenGetter,
  setApiUnauthorizedHandler,
} from "../api/request";

import { createAppState } from "./app/useAppState";
import { createFeedbackActions } from "./app/useFeedback";
import { createLayoutActions } from "./app/useLayoutActions";
import { createServiceActions } from "./app/useServiceActions";
import { createRouteActions } from "./app/useRouteActions";
import { createExploreActions } from "./app/useExploreActions";
import { createChatActions } from "./app/useChatActions";
import { createAuthActions } from "./app/useAuthActions";
import { createWeatherActions } from "./app/useWeatherActions";

export function useUserApp() {
  const ctx = createAppState();

  Object.assign(ctx, createFeedbackActions(ctx));
  Object.assign(ctx, createLayoutActions(ctx));
  Object.assign(ctx, createServiceActions(ctx));
  Object.assign(ctx, createRouteActions(ctx));
  Object.assign(ctx, createExploreActions(ctx));
  Object.assign(ctx, createWeatherActions(ctx));
  Object.assign(ctx, createChatActions(ctx));
  Object.assign(ctx, createAuthActions(ctx));

  setApiAuthTokenGetter(() => ctx.token.value);
  setApiUnauthorizedHandler(() => {
    ctx.clearSession();
    ctx.openAccountModal("account");
  });

  watch(
    () => ctx.routeEditor.value.days,
    (value) => {
      const nextDays = clamp(Number(value) || 1, 1, 14);
      if (ctx.routeEditor.value.days !== nextDays) {
        ctx.routeEditor.value.days = nextDays;
      }
      const dateRange = normalizeRouteDateRange({
        ...ctx.routeEditor.value,
        days: nextDays,
        endDate: addDaysToDate(ctx.routeEditor.value.startDate, nextDays - 1),
      });
      ctx.routeEditor.value.startDate = dateRange.startDate;
      ctx.routeEditor.value.endDate = dateRange.endDate;
      ctx.routeEditor.value.points = normalizeRoutePoints(ctx.routeEditor.value.points, nextDays);
    },
  );

  watch(ctx.token, (value) => {
    if (value) {
      localStorage.setItem(STORAGE_KEYS.token, value);
    } else {
      localStorage.removeItem(STORAGE_KEYS.token);
    }
  });

  watch(ctx.user, (value) => {
    if (value) {
      localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(value));
    } else {
      localStorage.removeItem(STORAGE_KEYS.user);
    }
  });

  watch([ctx.provider, ctx.model], () => {
    localStorage.setItem(STORAGE_KEYS.provider, ctx.provider.value || "");
    localStorage.setItem(STORAGE_KEYS.model, ctx.model.value || "");
  });

  watch(
    () => ctx.currentView.value,
    async (value) => {
      if (value !== "explore") {
        ctx.exploreRoutePlanDetail.value = null;
      }
      if (value === "chat") {
        ctx.closeAccountMenu();
        ctx.closeModelMenu();
        ctx.closeMobileSidebar();
      }
      if (value === "routes" && ctx.isAuthenticated.value && !ctx.loadingRoutePlans.value) {
        await ctx.loadRoutePlans();
      }
      if (
        value === "explore" &&
        ctx.isAuthenticated.value &&
        !ctx.loadingExplorePosts.value &&
        !ctx.explorePosts.value.length
      ) {
        await ctx.loadExplorePosts();
      }
      if (
        value === "explore" &&
        ctx.shouldLoadExploreRoutePlans(
          ctx.exploreTab.value,
          ctx.exploreMineView.value,
          ctx.exploreMineCategory.value,
        ) &&
        ctx.isAuthenticated.value &&
        !ctx.loadingRoutePlans.value &&
        !ctx.routePlans.value.length
      ) {
        await ctx.loadRoutePlans();
      }
    },
  );

  onMounted(async () => {
    ctx.syncHashView();
    ctx.syncViewport();
    window.addEventListener("resize", ctx.syncViewport);
    window.addEventListener("hashchange", ctx.syncHashView);
    window.addEventListener("keydown", ctx.handleWindowKeydown);
    document.addEventListener("mousedown", ctx.handleDocumentPointerDown);

    await ctx.loadPublicServiceStatus(true);
    ctx.startPublicServicePolling();
    await ctx.loadModels();
    if (!ctx.weatherServiceWarning.value) {
      ctx.requestCurrentLocation();
    }

    if (ctx.isAuthenticated.value) {
      await ctx.loadProfile();
      await ctx.loadConversations();
      await ctx.loadRoutePlans();
    } else {
      ctx.syncUserForms({});
    }
  });

  onBeforeUnmount(() => {
    ctx.stopStreamingResponse();
    ctx.closeConfirmDialog(false);
    window.removeEventListener("resize", ctx.syncViewport);
    window.removeEventListener("hashchange", ctx.syncHashView);
    window.removeEventListener("keydown", ctx.handleWindowKeydown);
    document.removeEventListener("mousedown", ctx.handleDocumentPointerDown);
    ctx.cleanupFeedback();
    ctx.stopPublicServicePolling();
  });

  return {
    budgetOptions,
    memoryOptions,
    starterPrompts,
    token: ctx.token,
    user: ctx.user,
    currentView: ctx.currentView,
    exploreTab: ctx.exploreTab,
    exploreMineView: ctx.exploreMineView,
    exploreMineCategory: ctx.exploreMineCategory,
    previousPrimaryView: ctx.previousPrimaryView,
    isSidebarCollapsed: ctx.isSidebarCollapsed,
    isMobileViewport: ctx.isMobileViewport,
    isMobileSidebarOpen: ctx.isMobileSidebarOpen,
    isSearchOpen: ctx.isSearchOpen,
    historyQuery: ctx.historyQuery,
    isModelMenuOpen: ctx.isModelMenuOpen,
    isAccountMenuOpen: ctx.isAccountMenuOpen,
    isAccountModalOpen: ctx.isAccountModalOpen,
    accountModalSection: ctx.accountModalSection,
    provider: ctx.provider,
    model: ctx.model,
    defaultProvider: ctx.defaultProvider,
    defaultModel: ctx.defaultModel,
    models: ctx.models,
    conversations: ctx.conversations,
    selectedConversationId: ctx.selectedConversationId,
    messages: ctx.messages,
    chatInput: ctx.chatInput,
    loadingConversations: ctx.loadingConversations,
    sendingChat: ctx.sendingChat,
    deletingConversationId: ctx.deletingConversationId,
    profileForm: ctx.profileForm,
    preferenceForm: ctx.preferenceForm,
    passwordForm: ctx.passwordForm,
    routePlans: ctx.routePlans,
    loadingRoutePlans: ctx.loadingRoutePlans,
    loadingRouteDetail: ctx.loadingRouteDetail,
    routeGenerating: ctx.routeGenerating,
    routeSaving: ctx.routeSaving,
    loadingExplorePosts: ctx.loadingExplorePosts,
    loadingExploreRoutePlanDetail: ctx.loadingExploreRoutePlanDetail,
    sharingRoutePlanId: ctx.sharingRoutePlanId,
    activeRoutePlanId: ctx.activeRoutePlanId,
    routeEditor: ctx.routeEditor,
    routeSnapshot: ctx.routeSnapshot,
    explorePosts: ctx.explorePosts,
    exploreRoutePlanDetail: ctx.exploreRoutePlanDetail,
    explorePublishedMarker: ctx.explorePublishedMarker,
    lastPublishedExplorePostId: ctx.lastPublishedExplorePostId,
    exploreNavigationMarker: ctx.exploreNavigationMarker,
    publicServices: ctx.publicServices,
    mapRuntimeConfig: ctx.mapRuntimeConfig,
    userLocation: ctx.userLocation,
    weatherForecast: ctx.weatherForecast,
    locatingUser: ctx.locatingUser,
    loadingWeather: ctx.loadingWeather,
    weatherError: ctx.weatherError,
    isWeatherPanelOpen: ctx.isWeatherPanelOpen,
    authTab: ctx.authTab,
    authLoading: ctx.authLoading,
    authError: ctx.authError,
    loginForm: ctx.loginForm,
    registerForm: ctx.registerForm,
    savingProfile: ctx.savingProfile,
    savingPreferences: ctx.savingPreferences,
    changingPassword: ctx.changingPassword,
    notice: ctx.notice,
    confirmDialog: ctx.confirmDialog,
    sidebarRef: ctx.sidebarRef,
    chatWorkspaceRef: ctx.chatWorkspaceRef,
    isAuthenticated: ctx.isAuthenticated,
    selectedModel: ctx.selectedModel,
    sortedModels: ctx.sortedModels,
    availableModels: ctx.availableModels,
    unavailableModels: ctx.unavailableModels,
    publicServiceStatusMap: ctx.publicServiceStatusMap,
    weatherServiceWarning: ctx.weatherServiceWarning,
    mapServiceWarning: ctx.mapServiceWarning,
    modelSelectionKey: ctx.modelSelectionKey,
    filteredConversations: ctx.filteredConversations,
    routeDirty: ctx.routeDirty,
    exploreFeedPosts: ctx.exploreFeedPosts,
    ownExplorePosts: ctx.ownExplorePosts,
    favoriteExplorePosts: ctx.favoriteExplorePosts,
    isAdmin: ctx.isAdmin,
    userDisplayName: ctx.userDisplayName,
    userEmailLabel: ctx.userEmailLabel,
    userInitial: ctx.userInitial,
    memoryLabel: ctx.memoryLabel,
    composerDisabled: ctx.composerDisabled,
    sendButtonDisabled: ctx.sendButtonDisabled,
    composerPlaceholder: ctx.composerPlaceholder,
    sidebarToggleTooltip: ctx.sidebarToggleTooltip,
    sidebarToggleIcon: ctx.sidebarToggleIcon,
    accountSectionMeta: ctx.accountSectionMeta,
    showNotice: ctx.showNotice,
    requestConfirm: ctx.requestConfirm,
    closeConfirmDialog: ctx.closeConfirmDialog,
    syncUserForms: ctx.syncUserForms,
    applyRouteResponse: ctx.applyRouteResponse,
    resetRouteEditor: ctx.resetRouteEditor,
    confirmDiscardRouteChanges: ctx.confirmDiscardRouteChanges,
    syncViewport: ctx.syncViewport,
    syncHashView: ctx.syncHashView,
    switchView: ctx.switchView,
    toggleSidebar: ctx.toggleSidebar,
    openMobileSidebar: ctx.openMobileSidebar,
    closeMobileSidebar: ctx.closeMobileSidebar,
    toggleModelMenu: ctx.toggleModelMenu,
    closeModelMenu: ctx.closeModelMenu,
    toggleAccountMenu: ctx.toggleAccountMenu,
    closeAccountMenu: ctx.closeAccountMenu,
    openAccountModal: ctx.openAccountModal,
    closeAccountModal: ctx.closeAccountModal,
    finishAuthSuccess: ctx.finishAuthSuccess,
    selectModelOption: ctx.selectModelOption,
    toggleSearch: ctx.toggleSearch,
    resetChatInput: ctx.resetChatInput,
    createChatMessage: ctx.createChatMessage,
    mapConversationMessages: ctx.mapConversationMessages,
    sleep: ctx.sleep,
    recoverInterruptedChatStream: ctx.recoverInterruptedChatStream,
    startNewChat: ctx.startNewChat,
    useStarterPrompt: ctx.useStarterPrompt,
    startFreshChatWithPrompt: ctx.startFreshChatWithPrompt,
    loadModels: ctx.loadModels,
    loadWeatherForecast: ctx.loadWeatherForecast,
    requestCurrentLocation: ctx.requestCurrentLocation,
    refreshWeather: ctx.refreshWeather,
    toggleWeatherPanel: ctx.toggleWeatherPanel,
    closeWeatherPanel: ctx.closeWeatherPanel,
    buildChatLocationPayload: ctx.buildChatLocationPayload,
    loadPublicServiceStatus: ctx.loadPublicServiceStatus,
    handleDocumentPointerDown: ctx.handleDocumentPointerDown,
    handleWindowKeydown: ctx.handleWindowKeydown,
    loadProfile: ctx.loadProfile,
    loadConversations: ctx.loadConversations,
    loadRoutePlans: ctx.loadRoutePlans,
    loadExplorePosts: ctx.loadExplorePosts,
    openRoutesView: ctx.openRoutesView,
    openExploreView: ctx.openExploreView,
    setExploreTab: ctx.setExploreTab,
    setExploreMineView: ctx.setExploreMineView,
    setExploreMineCategory: ctx.setExploreMineCategory,
    leaveExplore: ctx.leaveExplore,
    selectConversation: ctx.selectConversation,
    deleteConversation: ctx.deleteConversation,
    createRouteDraft: ctx.createRouteDraft,
    openRouteDraftFromMessage: ctx.openRouteDraftFromMessage,
    selectRoutePlan: ctx.selectRoutePlan,
    deleteRoutePlan: ctx.deleteRoutePlan,
    buildHistoryPayload: ctx.buildHistoryPayload,
    markRouteDirty: ctx.markRouteDirty,
    generateRoutePlan: ctx.generateRoutePlan,
    saveRoutePlan: ctx.saveRoutePlan,
    exportRoutePlan: ctx.exportRoutePlan,
    addRoutePoint: ctx.addRoutePoint,
    removeRoutePoint: ctx.removeRoutePoint,
    moveRoutePoint: ctx.moveRoutePoint,
    prependExplorePost: ctx.prependExplorePost,
    replaceExplorePost: ctx.replaceExplorePost,
    openExploreRoutePlanDetail: ctx.openExploreRoutePlanDetail,
    buildExploreSharedRoutePayload: ctx.buildExploreSharedRoutePayload,
    publishExplorePost: ctx.publishExplorePost,
    shareRoutePlanToExplore: ctx.shareRoutePlanToExplore,
    recordExploreRouteClick: ctx.recordExploreRouteClick,
    toggleExploreLike: ctx.toggleExploreLike,
    toggleExploreFavorite: ctx.toggleExploreFavorite,
    addExploreComment: ctx.addExploreComment,
    toggleExploreCommentLike: ctx.toggleExploreCommentLike,
    deleteExplorePost: ctx.deleteExplorePost,
    deleteExploreComment: ctx.deleteExploreComment,
    importSharedRoute: ctx.importSharedRoute,
    startPointChat: ctx.startPointChat,
    sendMessage: ctx.sendMessage,
    stopStreamingResponse: ctx.stopStreamingResponse,
    saveProfile: ctx.saveProfile,
    savePreferences: ctx.savePreferences,
    changePassword: ctx.changePassword,
    setAuthTab: ctx.setAuthTab,
    login: ctx.login,
    register: ctx.register,
    clearSession: ctx.clearSession,
    logout: ctx.logout,
    shouldLoadExploreRoutePlans: ctx.shouldLoadExploreRoutePlans,
    shouldKeepExploreRouteDetail: ctx.shouldKeepExploreRouteDetail,
  };
}
