<template>
  <aside class="app-sidebar">
    <div class="sidebar-head">
      <button
        type="button"
        class="logo-button"
        :data-tooltip="currentView === 'chat' ? '当前是聊天界面' : '返回聊天界面'"
        :title="currentView === 'chat' ? '当前是聊天界面' : '返回聊天界面'"
        @click="emit('switch-view', 'chat')"
      >
        <span class="logo-mark">
          <span class="logo-core"></span>
          <span class="logo-ring"></span>
          <span class="logo-dot"></span>
        </span>
      </button>

      <button
        type="button"
        class="icon-button"
        :data-tooltip="sidebarToggleTooltip"
        :title="sidebarToggleTooltip"
        :aria-label="sidebarToggleTooltip"
        @click="emit('toggle-sidebar')"
      >
        <AppIcon :name="sidebarToggleIcon" />
      </button>
    </div>

    <div class="sidebar-actions">
      <template v-if="currentView !== 'explore'">
        <button
          type="button"
          class="sidebar-action"
          :class="{ active: currentView === 'chat' && !selectedConversationId && !messagesLength }"
          data-tooltip="新聊天"
          title="新聊天"
          @click="emit('start-new-chat')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="square-pen" />
          </span>
          <span class="sidebar-action-label">新聊天</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: isSearchOpen }"
          data-tooltip="搜索聊天"
          title="搜索聊天"
          @click="emit('toggle-search')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="search" />
          </span>
          <span class="sidebar-action-label">搜索聊天</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: currentView === 'routes' }"
          data-tooltip="我的路线"
          title="我的路线"
          @click="emit('open-routes-view')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="map" />
          </span>
          <span class="sidebar-action-label">我的路线</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: currentView === 'explore' }"
          data-tooltip="探索社区"
          title="探索社区"
          @click="emit('open-explore-view')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="compass" />
          </span>
          <span class="sidebar-action-label">探索</span>
        </button>
      </template>

      <template v-else>
        <button
          type="button"
          class="sidebar-action"
          data-tooltip="返回"
          title="返回"
          @click="emit('leave-explore')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="arrow-left" />
          </span>
          <span class="sidebar-action-label">返回</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: exploreTab === 'discover' }"
          data-tooltip="发现"
          title="发现"
          @click="emit('set-explore-tab', 'discover')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="compass" />
          </span>
          <span class="sidebar-action-label">发现</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: exploreTab === 'routes' }"
          data-tooltip="路线"
          title="路线"
          @click="emit('set-explore-tab', 'routes')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="map" />
          </span>
          <span class="sidebar-action-label">路线</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: exploreTab === 'mine' }"
          data-tooltip="我的"
          title="我的"
          @click="emit('set-explore-tab', 'mine')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="bookmark" />
          </span>
          <span class="sidebar-action-label">我的</span>
        </button>

        <button
          type="button"
          class="sidebar-action"
          :class="{ active: exploreTab === 'map' }"
          data-tooltip="地图"
          title="地图"
          @click="emit('set-explore-tab', 'map')"
        >
          <span class="sidebar-action-icon">
            <AppIcon name="pin" />
          </span>
          <span class="sidebar-action-label">地图</span>
        </button>
      </template>
    </div>

    <transition name="fade-slide">
      <div
        v-if="currentView !== 'explore' && isSearchOpen && !isSidebarCollapsed"
        class="history-search"
      >
        <AppIcon name="search" class="history-search-icon" />
        <input
          ref="historySearchInput"
          v-model="historyQueryModel"
          type="text"
          placeholder="搜索聊天标题"
        />
      </div>
    </transition>

    <section class="history-section">
      <template v-if="currentView === 'explore'">
        <div class="history-section-head">
          <span v-if="!isSidebarCollapsed" class="history-section-title">探索导航</span>
          <span
            v-if="!isSidebarCollapsed && exploreTab === 'mine' && exploreMineView === 'favorites'"
            class="history-state"
          >
            {{ favoriteCount }} 条
          </span>
        </div>

        <span class="history-empty-dot"></span>
      </template>

      <template v-else>
        <div class="history-section-head">
          <span v-if="!isSidebarCollapsed" class="history-section-title">历史聊天</span>
          <span v-if="loadingConversations && !isSidebarCollapsed" class="history-state">加载中</span>
        </div>

        <div v-if="!isAuthenticated" class="history-empty">
          <p v-if="!isSidebarCollapsed">登录后同步历史聊天记录。</p>
          <button type="button" class="text-button" @click="emit('open-account-modal', 'account')">
            {{ isSidebarCollapsed ? "" : "去登录 / 注册" }}
            <AppIcon v-if="isSidebarCollapsed" name="user" />
          </button>
        </div>

        <div v-else-if="filteredConversations.length" class="history-list">
          <div
            v-for="item in filteredConversations"
            :key="item.id"
            class="history-card"
            :class="{ active: selectedConversationId === item.id }"
            :data-tooltip="historyTooltip(item)"
            :title="historyTooltip(item)"
          >
            <button
              type="button"
              class="history-card-main"
              @click="emit('select-conversation', item)"
            >
              <span class="history-card-copy">
                <strong>{{ item.title || "未命名对话" }}</strong>
              </span>
            </button>

            <button
              type="button"
              class="history-delete-button"
              :data-tooltip="`删除“${item.title || '未命名对话'}”`"
              :title="`删除“${item.title || '未命名对话'}”`"
              :aria-label="`删除“${item.title || '未命名对话'}”`"
              :disabled="deletingConversationId === item.id"
              @click.stop="emit('delete-conversation', item)"
            >
              <AppIcon name="trash" />
            </button>
          </div>
        </div>

        <div v-else class="history-empty">
          <p v-if="!isSidebarCollapsed">
            {{ historyQuery ? "没有匹配的聊天记录。" : "还没有历史聊天。" }}
          </p>
          <span v-else class="history-empty-dot"></span>
        </div>
      </template>
    </section>

    <div ref="accountMenuRoot" class="profile-entry-shell">
      <button
        type="button"
        class="profile-entry"
        :class="{ active: isAccountMenuOpen || isAccountModalOpen }"
        data-tooltip="个人信息"
        title="个人信息"
        @click="emit('toggle-account-menu')"
      >
        <span class="profile-avatar">{{ userInitial }}</span>
        <span class="profile-copy">
          <strong>{{ userDisplayName }}</strong>
          <span>{{ userEmailLabel }}</span>
        </span>
        <AppIcon
          class="profile-entry-chevron"
          name="chevron-down"
          :class="{ open: isAccountMenuOpen }"
        />
      </button>

      <transition name="fade-slide">
        <div v-if="isAccountMenuOpen" class="account-menu">
          <button
            type="button"
            class="account-menu-profile"
            @click="emit('open-account-modal', 'account')"
          >
            <span class="profile-avatar large">{{ userInitial }}</span>
            <span class="account-menu-profile-copy">
              <strong>{{ userDisplayName }}</strong>
              <span>{{ isAuthenticated ? userEmailLabel : "登录后同步聊天与偏好设置" }}</span>
            </span>
          </button>

          <div class="account-menu-group">
            <button type="button" class="account-menu-item" @click="emit('open-account-modal', 'preferences')">
              <AppIcon name="sliders" />
              <span>个性化</span>
            </button>
            <button type="button" class="account-menu-item" @click="emit('open-account-modal', 'account')">
              <AppIcon name="settings" />
              <span>设置</span>
            </button>
          </div>

          <div class="account-menu-group">
            <button
              v-if="isAuthenticated"
              type="button"
              class="account-menu-item danger"
              @click="emit('logout')"
            >
              <AppIcon name="logout" />
              <span>退出登录</span>
            </button>
            <button
              v-else
              type="button"
              class="account-menu-item"
              @click="emit('open-account-modal', 'account')"
            >
              <AppIcon name="user" />
              <span>登录 / 注册</span>
            </button>
          </div>
        </div>
      </transition>
    </div>
  </aside>
</template>

<script setup>
import { computed, nextTick, ref, watch } from "vue";

import AppIcon from "./AppIcon.vue";

const props = defineProps({
  currentView: {
    type: String,
    required: true,
  },
  isSearchOpen: {
    type: Boolean,
    default: false,
  },
  isSidebarCollapsed: {
    type: Boolean,
    default: false,
  },
  loadingConversations: {
    type: Boolean,
    default: false,
  },
  isAuthenticated: {
    type: Boolean,
    default: false,
  },
  filteredConversations: {
    type: Array,
    default: () => [],
  },
  selectedConversationId: {
    type: Number,
    default: null,
  },
  deletingConversationId: {
    type: Number,
    default: null,
  },
  historyQuery: {
    type: String,
    default: "",
  },
  messagesLength: {
    type: Number,
    default: 0,
  },
  isAccountMenuOpen: {
    type: Boolean,
    default: false,
  },
  isAccountModalOpen: {
    type: Boolean,
    default: false,
  },
  userInitial: {
    type: String,
    default: "U",
  },
  userDisplayName: {
    type: String,
    default: "未登录",
  },
  userEmailLabel: {
    type: String,
    default: "点击进入个人信息",
  },
  sidebarToggleTooltip: {
    type: String,
    required: true,
  },
  sidebarToggleIcon: {
    type: String,
    required: true,
  },
  exploreTab: {
    type: String,
    default: "discover",
  },
  exploreMineView: {
    type: String,
    default: "mine",
  },
  favoriteCount: {
    type: Number,
    default: 0,
  },
});

const emit = defineEmits([
  "delete-conversation",
  "leave-explore",
  "logout",
  "open-account-modal",
  "open-explore-view",
  "open-routes-view",
  "select-conversation",
  "set-explore-tab",
  "start-new-chat",
  "switch-view",
  "toggle-account-menu",
  "toggle-search",
  "toggle-sidebar",
  "update:historyQuery",
]);

const historySearchInput = ref(null);
const accountMenuRoot = ref(null);

const historyQueryModel = computed({
  get() {
    return props.historyQuery;
  },
  set(value) {
    emit("update:historyQuery", value);
  },
});

function historyTooltip(item) {
  return item?.title || "未命名对话";
}

watch(
  () => [props.isSearchOpen, props.isSidebarCollapsed],
  async ([open, collapsed]) => {
    if (open && !collapsed) {
      await nextTick();
      historySearchInput.value?.focus();
    }
  },
);

defineExpose({
  accountMenuRoot,
});
</script>
