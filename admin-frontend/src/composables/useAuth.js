import { ref, computed } from 'vue';
import { STORAGE_KEYS } from '@/constants/storage';
import { requestJson } from '@/utils/request';
import { storage } from '@/utils/storage';

// 全局单例状态
const token = ref(storage.get(STORAGE_KEYS.token) || '');
const user = ref(storage.get(STORAGE_KEYS.user));
const sessionReady = ref(false);

// 计算属性
const isAuthenticated = computed(() => Boolean(token.value && user.value));
const isAdmin = computed(() => Boolean(user.value?.admin));
const userDisplayName = computed(
  () => user.value?.displayName || user.value?.username || '管理员'
);
const userInitial = computed(() =>
  String(userDisplayName.value || 'A')
    .charAt(0)
    .toUpperCase()
);

// 清除会话
function clearSession() {
  token.value = '';
  user.value = null;
  storage.remove(STORAGE_KEYS.token);
  storage.remove(STORAGE_KEYS.user);
}

// 登录
async function login(account, password) {
  if (!account || !password) {
    throw new Error('请输入账号和密码。');
  }

  const data = await requestJson(
    '/api/auth/login',
    {
      method: 'POST',
      body: JSON.stringify({ account, password }),
    },
    false
  );

  if (!data?.user?.admin) {
    throw new Error('当前账号不是管理员，无法进入后台。');
  }

  token.value = data.token || '';
  user.value = data.user || null;

  // 持久化到 localStorage
  storage.set(STORAGE_KEYS.token, token.value);
  storage.set(STORAGE_KEYS.user, user.value);

  return data;
}

// 登出
function logout() {
  clearSession();
}

// 恢复会话
async function restoreSession() {
  if (!token.value) {
    sessionReady.value = true;
    return;
  }

  try {
    const profile = await requestJson('/api/users/me');
    user.value = profile;

    if (!profile?.admin) {
      clearSession();
    } else {
      // 更新 localStorage
      storage.set(STORAGE_KEYS.user, user.value);
    }
  } catch {
    clearSession();
  } finally {
    sessionReady.value = true;
  }
}

// 获取当前 token（用于请求拦截）
function getToken() {
  return token.value;
}

export function useAuth() {
  return {
    // 状态
    token,
    user,
    sessionReady,

    // 计算属性
    isAuthenticated,
    isAdmin,
    userDisplayName,
    userInitial,

    // 方法
    login,
    logout,
    restoreSession,
    getToken,
  };
}
