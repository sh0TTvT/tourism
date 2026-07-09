<template>
  <section class="auth-shell">
    <article class="auth-card intro-card">
      <p class="eyebrow">Tourism QA Admin</p>
      <h1>管理员控制台</h1>
      <p class="auth-copy">
        这是一个独立于用户端的新前端工程，专门用于后台用户管理、模型配置和知识图谱维护。
      </p>
      <ul class="auth-points">
        <li>独立运行在单独端口。</li>
        <li>只使用管理员鉴权访问后台接口。</li>
        <li>代码与用户端前端入口完全分离。</li>
      </ul>
    </article>

    <article class="auth-card login-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Admin Login</p>
          <h2>登录管理员账号</h2>
        </div>
      </div>

      <p v-if="authError" class="form-error">{{ authError }}</p>

      <form class="form-grid" @submit.prevent="handleLogin">
        <label>
          账号
          <input
            v-model="loginForm.account"
            type="text"
            autocomplete="username"
            placeholder="用户名或邮箱"
          />
        </label>

        <label>
          密码
          <input
            v-model="loginForm.password"
            type="password"
            autocomplete="current-password"
            placeholder="请输入密码"
          />
        </label>

        <button type="submit" class="primary-button" :disabled="authLoading">
          {{ authLoading ? "登录中..." : "进入管理员端" }}
        </button>
      </form>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { useAuth } from '@/composables/useAuth';

const { login } = useAuth();

const authLoading = ref(false);
const authError = ref('');
const loginForm = ref({
  account: '',
  password: '',
});

async function handleLogin() {
  authLoading.value = true;
  authError.value = '';

  try {
    await login(loginForm.value.account, loginForm.value.password);
    loginForm.value = { account: '', password: '' };
    // 登录成功后，父组件会通过 isAuthenticated 自动切换视图
  } catch (error) {
    authError.value = error.message;
  } finally {
    authLoading.value = false;
  }
}
</script>
