<template>
  <!-- 会话未就绪：显示加载状态 -->
  <section v-if="!sessionReady" class="auth-shell">
    <article class="auth-card compact">
      <p>正在检查管理员登录状态...</p>
    </article>
  </section>

  <!-- 未认证：显示登录表单 -->
  <LoginForm v-else-if="!isAuthenticated" />

  <!-- 已认证但不是管理员：显示拒绝访问 -->
  <section v-else-if="!isAdmin" class="auth-shell">
    <article class="auth-card compact">
      <p class="eyebrow">Access Denied</p>
      <h1>当前账号不是管理员</h1>
      <p class="auth-copy">这个独立后台只允许 `ADMIN` 角色访问。</p>
      <button type="button" class="primary-button" @click="logout">
        返回登录
      </button>
    </article>
  </section>

  <!-- 已认证且是管理员：显示受保护的内容 -->
  <slot v-else />
</template>

<script setup>
import { onMounted } from 'vue';
import { useAuth } from '@/composables/useAuth';
import LoginForm from './LoginForm.vue';

const { sessionReady, isAuthenticated, isAdmin, logout, restoreSession } = useAuth();

// 组件挂载时恢复会话
onMounted(() => {
  restoreSession();
});
</script>
