<template>
  <div class="admin-app">
    <!-- 使用 AuthGuard 包裹主内容 -->
    <AuthGuard>
      <div class="workspace-shell">
        <!-- 使用 Sidebar 组件 -->
        <Sidebar />

        <!-- 主面板 -->
        <main class="main-panel">
          <!-- 使用 Topbar 组件 -->
          <Topbar>
            <template #actions>
              <button type="button" class="ghost-button" @click="refreshActiveSection">
                刷新当前页
              </button>
            </template>
          </Topbar>

          <!-- 主内容区 -->
          <div class="main-content">
            <OverviewView v-if="activeSection === 'overview'" />

            <UsersView v-else-if="activeSection === 'users'" />

            <ModelsView v-else-if="activeSection === 'models'" />

          <ServicesView v-else-if="activeSection === 'services'" />

          <KnowledgeGraphView v-else />
          </div>
        </main>

        

      </div>
    </AuthGuard>

    <!-- Toast 通知 -->
    <transition name="toast">
      <div v-if="notice" class="toast" :class="notice.type">
        {{ notice.message }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, provide, ref, watch } from 'vue';

// 导入组件
import AuthGuard from '@/components/auth/AuthGuard.vue';
import Sidebar from '@/components/layout/Sidebar.vue';
import Topbar from '@/components/layout/Topbar.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import OverviewView from '@/views/overview/OverviewView.vue';
import UsersView from '@/views/users/UsersView.vue';
import ModelsView from '@/views/models/ModelsView.vue';
import ServicesView from '@/views/services/ServicesView.vue';
import KnowledgeGraphView from '@/views/knowledge-graph/KnowledgeGraphView.vue';

// 导入 composables
import { useAuth } from '@/composables/useAuth';
import { useNavigation } from '@/composables/useNavigation';

// 导入常量和工具
import { STORAGE_KEYS } from '@/constants/storage';
import { SECTIONS } from '@/constants/sections';
import { requestJson } from '@/utils/request';

// 使用 composables
const { user, logout: authLogout, getToken } = useAuth();
const { currentSection, navigateTo } = useNavigation();

// 重命名 activeSection 为与 currentSection 同步
const activeSection = currentSection;

// ========== 工具函数 ==========
function parseStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.user) || "null");
  } catch {
    return null;
  }
}

function formatError(payload, fallback = "请求失败，请稍后重试。") {
  if (!payload) {
    return fallback;
  }
  if (typeof payload === "string") {
    return payload;
  }
  if (typeof payload.message === "string" && payload.message) {
    return payload.message;
  }
  if (payload.fields && typeof payload.fields === "object") {
    const firstFieldError = Object.values(payload.fields)[0];
    return firstFieldError || fallback;
  }
  return fallback;
}

const adminUsers = ref([]);
const adminModels = ref([]);
const adminServices = ref([]);
const kgNodes = ref([]);
const kgRelationships = ref([]);

const loadingModels = ref(false);

const notice = ref(null);
let noticeTimer = 0;

const activeSectionMeta = computed(
  () => SECTIONS.find((item) => item.key === activeSection.value) || SECTIONS[0],
);
const bannedUsersCount = computed(
  () => adminUsers.value.filter((item) => item.banned).length,
);
const availableModelsCount = computed(
  () => adminModels.value.filter((item) => item.available).length,
);
const availableServicesCount = computed(
  () => adminServices.value.filter((item) => item.available).length,
);
const defaultModelsCount = computed(
  () => adminModels.value.filter((item) => item.defaultModel).length,
);
const problemUsers = computed(() =>
  adminUsers.value.filter((item) => item.banned || item.role === "ADMIN").slice(0, 5),
);

function showNotice(message, type = "info") {
  notice.value = { message, type };
  window.clearTimeout(noticeTimer);
  noticeTimer = window.setTimeout(() => {
    notice.value = null;
  }, 3200);
}

function logout() {
  authLogout(); // 调用 composable 的 logout
  adminUsers.value = [];
  adminModels.value = [];
  adminServices.value = [];
  showNotice("已退出管理员端。", "success");
}

function formatDateTime(value) {
  if (!value) {
    return "未知时间";
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function buildFallbackCover(seed) {
  const label = String(seed || "旅行内容").trim().slice(0, 2) || "旅行";
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 960 720">
      <defs>
        <linearGradient id="g" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#fb923c" />
          <stop offset="100%" stop-color="#f97316" />
        </linearGradient>
      </defs>
      <rect width="960" height="720" rx="48" fill="url(#g)" />
      <circle cx="120" cy="116" r="58" fill="rgba(255,255,255,0.18)" />
      <path d="M0 550 160 420l170 126 148-106 146 128 336-262v414H0Z" fill="rgba(255,247,237,0.68)" />
      <text x="64" y="606" fill="#fff7ed" font-size="76" font-family="PingFang SC, Microsoft YaHei, sans-serif" font-weight="700">${label}</text>
    </svg>
  `;
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
}

function handleEscapeClose(event) {
  if (event.key !== "Escape") {
    return;
  }
}

async function loadOverviewData() {
  await Promise.all([loadUsers(), loadModels(), loadKnowledgeGraph()]);
}

function refreshActiveSection() {
  const section = activeSection.value;
  if (section === 'overview') {
    loadOverviewData();
  } else if (section === 'users') {
    loadUsers();
  } else if (section === 'models') {
    loadModels();
  } else if (section === 'knowledge') {
    loadKnowledgeGraph();
  }
  showNotice("已刷新", "success");
}

async function loadUsers() {
  try {
    const data = await requestJson("/api/admin/users");
    adminUsers.value = Array.isArray(data) ? data : [];
  } catch (error) {
    showNotice(error.message, "error");
  }
}

function openUserSection(item) {
  navigateTo("users");
}

function openKnowledgeSection(item) {
  navigateTo("knowledge");
}

async function loadModels() {
  loadingModels.value = true;
  try {
    const data = await requestJson("/api/admin/models");
    adminModels.value = Array.isArray(data) ? data : [];
  } catch (error) {
    showNotice(error.message, "error");
  } finally {
    loadingModels.value = false;
  }
}

async function loadKnowledgeGraph() {
  try {
    // 只加载节点列表，关系数量从节点数据中推断
    const nodesData = await requestJson("/api/kg/nodes?limit=10");
    kgNodes.value = Array.isArray(nodesData) ? nodesData : [];
    // 关系数量暂时设为空数组，OverviewView 会显示 0
    kgRelationships.value = [];
  } catch (error) {
    console.error("加载知识图谱数据失败:", error);
    kgNodes.value = [];
    kgRelationships.value = [];
  }
}

// ========== 为子组件提供共享状态和方法 ==========
provide("adminUsers", adminUsers);
provide("adminModels", adminModels);
provide("adminServices", adminServices);
provide("kgNodes", kgNodes);
provide("kgRelationships", kgRelationships);
provide("bannedUsersCount", bannedUsersCount);
provide("availableModelsCount", availableModelsCount);
provide("availableServicesCount", availableServicesCount);
provide("defaultModelsCount", defaultModelsCount);
provide("problemUsers", problemUsers);
provide("goToSection", navigateTo); // 使用 useNavigation 的 navigateTo 方法
provide("openUserSection", openUserSection);
provide("openKnowledgeSection", openKnowledgeSection);
provide("showNotice", showNotice);
provide("requestJson", requestJson);
provide("formatDateTime", formatDateTime);
provide("loadModels", loadModels);

// 监听 currentSection 变化，触发数据加载
watch(currentSection, async (newSection, oldSection) => {
  if (newSection === oldSection) return;

  if (newSection === 'overview') {
    await loadOverviewData();
  } else if (newSection === 'users' && !adminUsers.value.length) {
    await loadUsers();
  } else if (newSection === 'models' && !adminModels.value.length) {
    await loadModels();
  } else if (newSection === 'knowledge' && !kgNodes.value.length) {
    await loadKnowledgeGraph();
  }
});

onMounted(() => {
  window.addEventListener("keydown", handleEscapeClose);
});

onBeforeUnmount(() => {
  window.removeEventListener("keydown", handleEscapeClose);
});

</script>
