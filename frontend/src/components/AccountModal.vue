<template>
  <div class="account-modal-backdrop" @click.self="emit('close')">
    <section
      class="account-modal"
      :class="{ 'auth-mode': !isAuthenticated }"
      role="dialog"
      aria-modal="true"
      aria-label="账户设置"
    >
      <button
        type="button"
        class="account-modal-close"
        aria-label="关闭账户设置"
        @click="emit('close')"
      >
        <AppIcon name="close" />
      </button>

      <template v-if="isAuthenticated">
        <aside class="account-modal-sidebar">
          <button
            type="button"
            class="account-nav-item"
            :class="{ active: section === 'account' }"
            @click="emit('set-section', 'account')"
          >
            <AppIcon name="settings" />
            <span>常规</span>
          </button>
          <button
            type="button"
            class="account-nav-item"
            :class="{ active: section === 'preferences' }"
            @click="emit('set-section', 'preferences')"
          >
            <AppIcon name="sliders" />
            <span>个性化</span>
          </button>
          <button
            type="button"
            class="account-nav-item"
            :class="{ active: section === 'security' }"
            @click="emit('set-section', 'security')"
          >
            <AppIcon name="shield" />
            <span>安全</span>
          </button>
        </aside>

        <div class="account-modal-main">
          <header class="profile-header modal">
            <div>
              <p>{{ accountSectionMeta.label }}</p>
              <h1>{{ accountSectionMeta.title }}</h1>
            </div>
          </header>

          <div
            ref="contentFrame"
            class="account-modal-content"
            :style="{ '--account-content-height': contentHeight }"
          >
            <Transition
              name="account-section"
              mode="out-in"
              @after-enter="updateContentHeight"
            >
              <div
                :key="section"
                ref="sectionPanel"
                class="account-section-scroll"
              >
                <div v-if="section === 'account'" class="profile-grid modal">
                  <article class="profile-card profile-overview">
                    <div class="profile-overview-top">
                      <span class="profile-overview-avatar">{{
                        userInitial
                      }}</span>
                      <div>
                        <h2>{{ userDisplayName }}</h2>
                        <p>{{ userEmailLabel }}</p>
                      </div>
                    </div>

                    <dl class="profile-overview-meta">
                      <div>
                        <dt>用户名</dt>
                        <dd>{{ user?.username || "未设置" }}</dd>
                      </div>
                      <div>
                        <dt>记忆策略</dt>
                        <dd>{{ memoryLabel }}</dd>
                      </div>
                      <div>
                        <dt>常用出发地</dt>
                        <dd>
                          {{ preferenceForm.preferredDeparture || "未设置" }}
                        </dd>
                      </div>
                    </dl>
                  </article>

                  <article class="profile-card">
                    <div class="card-head">
                      <div>
                        <p>基础资料</p>
                        <h2>个人资料</h2>
                      </div>
                    </div>

                    <form
                      class="form-grid"
                      @submit.prevent="emit('save-profile')"
                    >
                      <label>
                        昵称
                        <input
                          v-model="profileForm.displayName"
                          type="text"
                          maxlength="60"
                        />
                      </label>
                      <label>
                        邮箱（选填）
                        <input
                          v-model="profileForm.email"
                          type="email"
                          maxlength="120"
                        />
                      </label>

                      <button
                        type="submit"
                        class="primary-button"
                        :disabled="savingProfile"
                      >
                        {{ savingProfile ? "保存中..." : "保存个人资料" }}
                      </button>
                    </form>
                  </article>
                </div>

                <article
                  v-else-if="section === 'preferences'"
                  class="profile-card modal-section-card"
                >
                  <div class="card-head">
                    <div>
                      <p>偏好设置</p>
                      <h2>旅行偏好</h2>
                    </div>
                  </div>

                  <form
                    class="form-grid"
                    @submit.prevent="emit('save-preferences')"
                  >
                    <label>
                      常用出发地
                      <input
                        v-model="preferenceForm.preferredDeparture"
                        type="text"
                        maxlength="120"
                      />
                    </label>

                    <div class="form-field">
                      <span>预算偏好</span>
                      <div
                        class="custom-select"
                        :class="{ open: openSelect === 'budget' }"
                      >
                        <button
                          type="button"
                          class="custom-select-trigger"
                          @click.stop="toggleSelect('budget')"
                        >
                          <span>{{ budgetLabel }}</span>
                          <span class="custom-select-arrow">⌄</span>
                        </button>
                        <div
                          v-if="openSelect === 'budget'"
                          class="custom-select-menu"
                        >
                          <button type="button" @click="selectBudget('')">
                            未指定
                          </button>
                          <button
                            v-for="item in budgetOptions"
                            :key="item"
                            type="button"
                            @click="selectBudget(item)"
                          >
                            {{ item }}
                          </button>
                        </div>
                      </div>
                    </div>

                    <label>
                      出行偏好
                      <textarea
                        v-model="preferenceForm.travelPreferences"
                        rows="4"
                        maxlength="255"
                        placeholder="例如：喜欢慢节奏、步行友好、夜景和本地美食"
                      ></textarea>
                    </label>

                    <label>
                      兴趣标签
                      <input
                        v-model="preferenceForm.interestTags"
                        type="text"
                        maxlength="255"
                        placeholder="例如：摄影, 美食, 海边"
                      />
                    </label>

                    <div class="form-field">
                      <span>记忆策略</span>
                      <div
                        class="custom-select"
                        :class="{ open: openSelect === 'memory' }"
                      >
                        <button
                          type="button"
                          class="custom-select-trigger"
                          @click.stop="toggleSelect('memory')"
                        >
                          <span>{{ selectedMemoryLabel }}</span>
                          <span class="custom-select-arrow">⌄</span>
                        </button>
                        <div
                          v-if="openSelect === 'memory'"
                          class="custom-select-menu"
                        >
                          <button
                            v-for="item in memoryOptions"
                            :key="item.value"
                            type="button"
                            @click="selectMemory(item.value)"
                          >
                            {{ item.label }}
                          </button>
                        </div>
                      </div>
                    </div>

                    <button
                      type="submit"
                      class="primary-button"
                      :disabled="savingPreferences"
                    >
                      {{ savingPreferences ? "保存中..." : "保存偏好设置" }}
                    </button>
                  </form>
                </article>

                <article v-else class="profile-card modal-section-card">
                  <div class="card-head">
                    <div>
                      <p>安全设置</p>
                      <h2>修改密码</h2>
                    </div>
                  </div>

                  <form
                    class="form-grid"
                    @submit.prevent="emit('change-password')"
                  >
                    <label>
                      当前密码
                      <input
                        v-model="passwordForm.currentPassword"
                        type="password"
                      />
                    </label>

                    <label>
                      新密码
                      <input
                        v-model="passwordForm.newPassword"
                        type="password"
                      />
                    </label>

                    <label>
                      确认新密码
                      <input
                        v-model="passwordForm.confirmPassword"
                        type="password"
                      />
                    </label>

                    <button
                      type="submit"
                      class="primary-button"
                      :disabled="changingPassword"
                    >
                      {{ changingPassword ? "更新中..." : "更新密码" }}
                    </button>
                  </form>
                </article>
              </div>
            </Transition>
          </div>
        </div>
      </template>

      <div v-else class="account-modal-auth">
        <article class="auth-card auth-intro">
          <p>统一账号</p>
          <h2>登录后同步聊天历史、模型偏好和个人资料</h2>
          <ul>
            <li>左侧聊天历史会自动从后端加载。</li>
            <li>可在弹窗中修改昵称、邮箱、旅行偏好和密码。</li>
            <li>登录后新聊天和模型切换会保持一致的使用体验。</li>
          </ul>
        </article>

        <article class="auth-card auth-form-card">
          <div class="auth-tabs">
            <button
              type="button"
              :class="{ active: authTab === 'login' }"
              @click="emit('set-auth-tab', 'login')"
            >
              登录
            </button>
            <button
              type="button"
              :class="{ active: authTab === 'register' }"
              @click="emit('set-auth-tab', 'register')"
            >
              注册
            </button>
          </div>

          <div
            ref="authFrame"
            class="auth-form-frame"
            :style="{ '--auth-form-height': authContentHeight }"
          >
            <Transition
              name="auth-form"
              mode="out-in"
              @after-enter="updateAuthContentHeight"
            >
              <div :key="authTab" ref="authPanel" class="auth-form-panel">
                <p v-if="authError" class="auth-error">{{ authError }}</p>

                <form
                  v-if="authTab === 'login'"
                  class="form-grid"
                  @submit.prevent="emit('login')"
                >
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
                    />
                  </label>

                  <button
                    type="submit"
                    class="primary-button"
                    :disabled="authLoading"
                  >
                    {{ authLoading ? "登录中..." : "登录" }}
                  </button>
                </form>

                <form
                  v-else
                  class="form-grid"
                  @submit.prevent="emit('register')"
                >
                  <label>
                    用户名
                    <input
                      v-model="registerForm.username"
                      type="text"
                      autocomplete="username"
                      placeholder="字母开头，可包含数字和下划线"
                    />
                  </label>

                  <label>
                    昵称
                    <input
                      v-model="registerForm.displayName"
                      type="text"
                      autocomplete="nickname"
                    />
                  </label>

                  <label>
                    邮箱（选填）
                    <input
                      v-model="registerForm.email"
                      type="email"
                      autocomplete="email"
                    />
                  </label>

                  <label>
                    密码
                    <input
                      v-model="registerForm.password"
                      type="password"
                      autocomplete="new-password"
                      placeholder="至少 6 位，仅支持字母和数字"
                    />
                  </label>

                  <button
                    type="submit"
                    class="primary-button"
                    :disabled="authLoading"
                  >
                    {{ authLoading ? "注册中..." : "注册并登录" }}
                  </button>
                </form>
              </div>
            </Transition>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch,
} from "vue";
import AppIcon from "./AppIcon.vue";

const props = defineProps({
  isAuthenticated: {
    type: Boolean,
    default: false,
  },
  section: {
    type: String,
    default: "account",
  },
  accountSectionMeta: {
    type: Object,
    required: true,
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
  user: {
    type: Object,
    default: null,
  },
  memoryLabel: {
    type: String,
    default: "标准记忆",
  },
  profileForm: {
    type: Object,
    required: true,
  },
  preferenceForm: {
    type: Object,
    required: true,
  },
  passwordForm: {
    type: Object,
    required: true,
  },
  authTab: {
    type: String,
    default: "login",
  },
  authLoading: {
    type: Boolean,
    default: false,
  },
  authError: {
    type: String,
    default: "",
  },
  loginForm: {
    type: Object,
    required: true,
  },
  registerForm: {
    type: Object,
    required: true,
  },
  budgetOptions: {
    type: Array,
    default: () => [],
  },
  memoryOptions: {
    type: Array,
    default: () => [],
  },
  savingProfile: {
    type: Boolean,
    default: false,
  },
  savingPreferences: {
    type: Boolean,
    default: false,
  },
  changingPassword: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  "change-password",
  "close",
  "login",
  "register",
  "save-preferences",
  "save-profile",
  "set-auth-tab",
  "set-section",
]);

const openSelect = ref("");
const contentHeight = ref("auto");
const sectionPanel = ref(null);
const contentFrame = ref(null);

const authContentHeight = ref("auto");
const authPanel = ref(null);
const authFrame = ref(null);

const budgetLabel = computed(
  () => props.preferenceForm.budgetPreference || "未指定",
);
const selectedMemoryLabel = computed(() => {
  const matched = props.memoryOptions.find(
    (item) => item.value === props.preferenceForm.memoryStrategy,
  );
  return matched?.label || "标准记忆";
});

const closeSelect = () => {
  openSelect.value = "";
};

const toggleSelect = (name) => {
  openSelect.value = openSelect.value === name ? "" : name;
};

const selectBudget = (value) => {
  props.preferenceForm.budgetPreference = value;
  closeSelect();
  updateContentHeight();
};

const selectMemory = (value) => {
  props.preferenceForm.memoryStrategy = value;
  closeSelect();
  updateContentHeight();
};

const updateContentHeight = async () => {
  await nextTick();
  if (!sectionPanel.value || !contentFrame.value) return;

  const maxHeightValue = Number.parseFloat(
    window.getComputedStyle(contentFrame.value).maxHeight,
  );

  // 不要测 account-section-scroll 自身，因为它有 height: 100%，会保留旧高度
  // 应该测里面真正的表单内容高度
  const contentEl = sectionPanel.value.firstElementChild || sectionPanel.value;
  const measuredHeight = contentEl.scrollHeight;

  const targetHeight = Number.isFinite(maxHeightValue)
    ? Math.min(measuredHeight, maxHeightValue)
    : measuredHeight;

  contentHeight.value = `${Math.ceil(targetHeight)}px`;
};

const updateAuthContentHeight = async () => {
  await nextTick();
  if (!authPanel.value || !authFrame.value) return;

  const contentEl = authPanel.value.firstElementChild
    ? authPanel.value
    : authPanel.value;

  const measuredHeight = contentEl.scrollHeight;
  authContentHeight.value = `${Math.ceil(measuredHeight)}px`;
};

watch(
  () => props.section,
  async () => {
    closeSelect();
    await updateContentHeight();
  },
);

watch(
  () => props.authTab,
  async () => {
    await updateAuthContentHeight();
  },
);

watch(
  () => props.authError,
  async () => {
    await updateAuthContentHeight();
  },
);

onMounted(() => {
  document.addEventListener("click", closeSelect);
  window.addEventListener("resize", updateContentHeight);
  window.addEventListener("resize", updateAuthContentHeight);
  updateContentHeight();
  updateAuthContentHeight();
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeSelect);
  window.removeEventListener("resize", updateContentHeight);
  window.removeEventListener("resize", updateAuthContentHeight);
});
</script>
