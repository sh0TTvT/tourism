import { computed, ref } from "vue";

import {
  STORAGE_KEYS,
  memoryOptions,
} from "../../constants/uiOptions";
import { createRouteEditor, serializeRouteEditor } from "../../utils/routePlanner";
import { decorateExplorePosts } from "../../utils/exploreFeed";
import { createDefaultMapConfig, parseStoredUser } from "./helpers";

export function createAppState() {
  const token = ref(localStorage.getItem(STORAGE_KEYS.token) || "");
  const user = ref(parseStoredUser());
  const currentView = ref("chat");
  const exploreTab = ref("discover");
  const exploreMineView = ref("mine");
  const exploreMineCategory = ref("posts");
  const previousPrimaryView = ref("chat");
  const isSidebarCollapsed = ref(false);
  const isMobileViewport = ref(false);
  const isMobileSidebarOpen = ref(false);
  const isSearchOpen = ref(false);
  const historyQuery = ref("");
  const isModelMenuOpen = ref(false);
  const isAccountMenuOpen = ref(false);
  const isAccountModalOpen = ref(window.location.hash.includes("profile"));
  const accountModalSection = ref("account");

  const provider = ref(localStorage.getItem(STORAGE_KEYS.provider) || "");
  const model = ref(localStorage.getItem(STORAGE_KEYS.model) || "");
  const defaultProvider = ref("");
  const defaultModel = ref("");
  const models = ref([]);
  const conversations = ref([]);
  const selectedConversationId = ref(null);
  const messages = ref([]);
  const chatInput = ref("");
  const loadingConversations = ref(false);
  const sendingChat = ref(false);
  const deletingConversationId = ref(null);

  const profileForm = ref({ displayName: "", email: "" });
  const preferenceForm = ref({
    preferredDeparture: "",
    budgetPreference: "",
    travelPreferences: "",
    interestTags: "",
    memoryStrategy: "STANDARD",
  });
  const passwordForm = ref({ currentPassword: "", newPassword: "", confirmPassword: "" });

  const routePlans = ref([]);
  const loadingRoutePlans = ref(false);
  const loadingRouteDetail = ref(false);
  const routeGenerating = ref(false);
  const routeSaving = ref(false);
  const loadingExplorePosts = ref(false);
  const loadingExploreRoutePlanDetail = ref(false);
  const sharingRoutePlanId = ref(null);
  const activeRoutePlanId = ref(null);
  const routeEditor = ref(createRouteEditor({}, preferenceForm.value));
  const routeSnapshot = ref(serializeRouteEditor(routeEditor.value));
  const explorePosts = ref([]);
  const exploreRoutePlanDetail = ref(null);
  const explorePublishedMarker = ref(0);
  const lastPublishedExplorePostId = ref(null);
  const exploreNavigationMarker = ref(0);
  const publicServices = ref([]);
  const mapRuntimeConfig = ref(createDefaultMapConfig());
  const userLocation = ref(null);
  const weatherForecast = ref(null);
  const locatingUser = ref(false);
  const loadingWeather = ref(false);
  const weatherError = ref("");
  const isWeatherPanelOpen = ref(false);

  const authTab = ref("login");
  const authLoading = ref(false);
  const authError = ref("");
  const loginForm = ref({ account: "", password: "" });
  const registerForm = ref({ username: "", displayName: "", email: "", password: "" });

  const savingProfile = ref(false);
  const savingPreferences = ref(false);
  const changingPassword = ref(false);

  const notice = ref(null);
  const confirmDialog = ref(null);
  const sidebarRef = ref(null);
  const chatWorkspaceRef = ref(null);

  const isAuthenticated = computed(() => Boolean(token.value && user.value));
  const selectedModel = computed(() =>
    models.value.find((item) => item.provider === provider.value && item.id === model.value) || null,
  );

  function isDefaultModel(item) {
    return item?.provider === defaultProvider.value && item?.id === defaultModel.value;
  }
  function isSelectedModel(item) {
    return item?.provider === provider.value && item?.id === model.value;
  }
  function modelSortScore(item) {
    if (isSelectedModel(item)) return 0;
    if (isDefaultModel(item) && item.available) return 1;
    if (item.available) return 2;
    return 3;
  }

  const sortedModels = computed(() =>
    [...models.value].sort((left, right) => {
      const leftScore = modelSortScore(left);
      const rightScore = modelSortScore(right);
      if (leftScore !== rightScore) return leftScore - rightScore;
      return String(left.displayName || left.id).localeCompare(String(right.displayName || right.id), "zh-CN");
    }),
  );
  const availableModels = computed(() => sortedModels.value.filter((item) => item.available));
  const unavailableModels = computed(() => sortedModels.value.filter((item) => !item.available));
  const publicServiceStatusMap = computed(() =>
    Object.fromEntries(publicServices.value.filter((item) => item?.serviceKey).map((item) => [item.serviceKey, item])),
  );
  const weatherServiceWarning = computed(() => {
    const item = publicServiceStatusMap.value.weather;
    if (!item || item.available) return "";
    return item.enabled ? "天气服务暂时不可用，当前无法提供实时天气查询结果。" : "天气服务当前已关闭。";
  });
  const mapServiceWarning = computed(() => {
    const item = publicServiceStatusMap.value.map;
    if (!item || item.available) return "";
    return item.message || (item.enabled ? "地图服务当前不可用，地图预览、选点和坐标解析可能受影响。" : "地图服务当前已关闭。");
  });
  const modelSelectionKey = computed({
    get() {
      return provider.value && model.value ? `${provider.value}::${model.value}` : "";
    },
    set(value) {
      const [nextProvider, ...rest] = String(value || "").split("::");
      const nextModel = rest.join("::");
      if (!nextProvider || !nextModel) return;
      provider.value = nextProvider;
      model.value = nextModel;
    },
  });
  const filteredConversations = computed(() => {
    const keyword = historyQuery.value.trim().toLowerCase();
    if (!keyword) return conversations.value;
    return conversations.value.filter((item) =>
      [item.title, item.provider, item.model].some((field) => String(field || "").toLowerCase().includes(keyword)),
    );
  });
  const routeDirty = computed(() => serializeRouteEditor(routeEditor.value) !== routeSnapshot.value);
  const exploreFeedPosts = computed(() => decorateExplorePosts(explorePosts.value));
  const ownExplorePosts = computed(() => exploreFeedPosts.value.filter((post) => post.isOwn));
  const favoriteExplorePosts = computed(() => exploreFeedPosts.value.filter((post) => post.liked || post.favorited));
  const isAdmin = computed(() => String(user.value?.role || "").toUpperCase() === "ADMIN");
  const userDisplayName = computed(() => user.value?.displayName || user.value?.username || "未登录");
  const userEmailLabel = computed(() => user.value?.email || "点击进入个人信息");
  const userInitial = computed(() => String(userDisplayName.value || "U").trim().slice(0, 1).toUpperCase());
  const memoryLabel = computed(() => {
    const matched = memoryOptions.find((item) => item.value === preferenceForm.value.memoryStrategy);
    return matched?.label || "标准记忆";
  });
  const composerDisabled = computed(() => sendingChat.value || !isAuthenticated.value || !selectedModel.value || !selectedModel.value.available);
  const sendButtonDisabled = computed(() => !sendingChat.value && (!isAuthenticated.value || !selectedModel.value || !selectedModel.value.available));
  const composerPlaceholder = computed(() => {
    if (!isAuthenticated.value) return "登录后开始提问";
    if (!selectedModel.value) return "正在加载模型列表...";
    if (!selectedModel.value.available) return selectedModel.value.reason || "当前模型不可用";
    return "给旅游助手发送消息";
  });
  const sidebarToggleTooltip = computed(() => {
    if (isMobileViewport.value) return isMobileSidebarOpen.value ? "关闭边栏" : "打开边栏";
    return isSidebarCollapsed.value ? "展开边栏" : "关闭边栏";
  });
  const sidebarToggleIcon = computed(() => {
    if (isMobileViewport.value) return isMobileSidebarOpen.value ? "panel-close" : "panel-open";
    return isSidebarCollapsed.value ? "panel-open" : "panel-close";
  });
  const accountSectionMeta = computed(() => {
    const sectionMap = {
      account: { label: "常规", title: "账号设置" },
      preferences: { label: "个性化", title: "旅行偏好" },
      security: { label: "安全", title: "账户安全" },
    };
    return sectionMap[accountModalSection.value] || sectionMap.account;
  });

  return {
    token, user, currentView, exploreTab, exploreMineView, exploreMineCategory, previousPrimaryView,
    isSidebarCollapsed, isMobileViewport, isMobileSidebarOpen, isSearchOpen, historyQuery,
    isModelMenuOpen, isAccountMenuOpen, isAccountModalOpen, accountModalSection,
    provider, model, defaultProvider, defaultModel, models, conversations, selectedConversationId,
    messages, chatInput, loadingConversations, sendingChat, deletingConversationId,
    profileForm, preferenceForm, passwordForm, routePlans, loadingRoutePlans, loadingRouteDetail,
    routeGenerating, routeSaving, loadingExplorePosts, loadingExploreRoutePlanDetail, sharingRoutePlanId,
    activeRoutePlanId, routeEditor, routeSnapshot, explorePosts, exploreRoutePlanDetail, explorePublishedMarker,
    lastPublishedExplorePostId, exploreNavigationMarker, publicServices, mapRuntimeConfig,
    userLocation, weatherForecast, locatingUser, loadingWeather, weatherError, isWeatherPanelOpen,
    authTab, authLoading, authError, loginForm, registerForm, savingProfile, savingPreferences, changingPassword,
    notice, confirmDialog, sidebarRef, chatWorkspaceRef,
    isAuthenticated, selectedModel, sortedModels, availableModels, unavailableModels, publicServiceStatusMap,
    weatherServiceWarning, mapServiceWarning, modelSelectionKey, filteredConversations, routeDirty,
    exploreFeedPosts, ownExplorePosts, favoriteExplorePosts, isAdmin, userDisplayName, userEmailLabel,
    userInitial, memoryLabel, composerDisabled, sendButtonDisabled, composerPlaceholder,
    sidebarToggleTooltip, sidebarToggleIcon, accountSectionMeta,
  };
}
