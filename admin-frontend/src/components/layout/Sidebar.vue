<template>
  <aside class="sidebar">
    <div class="brand-block">
      <p class="eyebrow">Tourism QA</p>
      <h1>Admin Console</h1>
    </div>

    <nav class="nav-stack">
      <button
        v-for="item in SECTIONS"
        :key="item.key"
        type="button"
        class="nav-item"
        :class="{ active: currentSection === item.key }"
        @click="navigateTo(item.key)"
      >
        <AppIcon :name="item.icon" />
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <div class="sidebar-bottom">
      <div class="identity-card">
        <span class="avatar">{{ userInitial }}</span>
        <div>
          <strong>{{ userDisplayName }}</strong>
          <span>{{ user?.email || user?.username }}</span>
        </div>
      </div>

      <button type="button" class="ghost-button full" @click="handleLogout">
        退出登录
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { SECTIONS } from '@/constants/sections';
import { useNavigation } from '@/composables/useNavigation';
import { useAuth } from '@/composables/useAuth';

const { currentSection, navigateTo } = useNavigation();
const { user, logout } = useAuth();

const userDisplayName = computed(
  () => user.value?.displayName || user.value?.username || '管理员'
);

const userInitial = computed(() =>
  String(userDisplayName.value || 'A')
    .trim()
    .slice(0, 1)
    .toUpperCase()
);

function handleLogout() {
  logout();
}
</script>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  z-index: 12;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at top right, rgba(15, 157, 122, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 248, 252, 0.98) 100%);
}

.nav-stack {
  display: grid;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  border-radius: 16px;
  color: var(--text);
  cursor: pointer;
  transition: background-color 0.18s ease, transform 0.18s ease;
}

.nav-item:hover,
.nav-item.active {
  background: rgba(15, 157, 122, 0.1);
  transform: translateX(2px);
}

.nav-item svg {
  width: 18px;
  height: 18px;
  flex: none;
}

.sidebar-bottom {
  margin-top: auto;
  display: grid;
  gap: 12px;
  flex-shrink: 0;
}

.identity-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.88);
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  background: linear-gradient(135deg, #0b7d60 0%, #12b48a 100%);
}

.identity-card div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.identity-card strong,
.identity-card span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identity-card span {
  color: var(--text-muted);
  font-size: 12px;
}
</style>
