<template>
  <section class="content-grid two-col">
    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Admin Users</p>
          <h3>用户列表</h3>
        </div>
        <button type="button" class="ghost-button" @click="loadUsers">
          刷新
        </button>
      </div>

      <div class="search-row">
        <span class="search-input-wrap">
          <input
            v-model="userSearch"
            type="text"
            placeholder="搜索用户名、昵称、邮箱"
            @keydown.enter.prevent="userSearchTerm = userSearch; userForm = createUserForm()"
          />
          <button
            v-if="userSearch"
            type="button"
            class="search-clear"
            @click="userSearch = ''; userSearchTerm = ''; userForm = createUserForm()"
          >
            <AppIcon name="close" />
          </button>
        </span>
        <button type="button" class="primary-button slim" @click="userSearchTerm = userSearch; userForm = createUserForm()">
          搜索
        </button>
      </div>

      <div v-if="loadingUsers" class="empty-copy">正在加载用户...</div>
      <div v-else-if="filteredUsers.length" class="list-stack">
        <button
          v-for="item in filteredUsers"
          :key="item.id"
          type="button"
          class="list-item"
          :class="{ active: userForm.id === item.id }"
          @click="applyUser(item)"
        >
          <div>
            <strong>{{ item.displayName || item.username }}</strong>
            <span>{{ item.email }}</span>
          </div>
          <em>{{ item.banned ? "封禁" : item.role }}</em>
        </button>
      </div>
      <p v-else class="empty-copy">暂无用户数据。</p>
    </article>

    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Editor</p>
          <h3>用户编辑</h3>
        </div>
      </div>

      <form v-if="userForm.id" class="form-grid surface-form" @submit.prevent="saveUser">
        <label>
          用户名
          <input :value="userForm.username" type="text" disabled />
        </label>

        <label>
          昵称
          <input v-model="userForm.displayName" type="text" maxlength="60" />
        </label>

        <label>
          邮箱
          <input v-model="userForm.email" type="email" maxlength="120" />
        </label>

        <label>
          角色
          <select v-model="userForm.role">
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </label>

        <label class="checkbox-row">
          <input v-model="userForm.banned" type="checkbox" />
          <span>封禁该账号</span>
        </label>

        <label>
          封禁原因
          <textarea
            v-model="userForm.banReason"
            rows="4"
            maxlength="255"
            placeholder="可选，记录封禁原因"
          ></textarea>
        </label>

        <div class="button-row">
          <button type="submit" class="primary-button" :disabled="savingUser">
            {{ savingUser ? "保存中..." : "保存用户信息" }}
          </button>
          <button
            type="button"
            class="ghost-button"
            :disabled="loadingUserContent"
            @click="openUserPublishedContent"
          >
            {{ loadingUserContent ? "加载中..." : "查看发布内容" }}
          </button>
        </div>
      </form>
      <p v-else class="empty-copy">从左侧选择一个用户后再编辑。</p>
    </article>
  </section>

  <!-- 用户发布内容弹窗 - 使用 Teleport 渲染到 body -->
  <Teleport to="body">
    <div v-if="showContentModal" class="modal-overlay" @click.self="closeContentModal">
      <div class="modal-content large">
        <div class="modal-header">
          <h3>用户发布内容</h3>
          <button type="button" class="icon-button" @click="closeContentModal">
            <AppIcon name="close" />
          </button>
        </div>
        <div class="modal-body">
          <div v-if="loadingUserContent" class="empty-copy">正在加载...</div>
          <div v-else-if="userPublishedContent">
            <!-- 帖子 -->
            <section v-if="userPublishedContent.posts.length > 0" class="content-section">
              <h4>帖子 ({{ userPublishedContent.posts.length }})</h4>
              <div class="content-list">
                <div v-for="post in userPublishedContent.posts" :key="post.id" class="content-item">
                  <div class="content-header">
                    <strong>{{ post.title }}</strong>
                    <span class="content-meta">{{ formatDate(post.createdAt) }}</span>
                  </div>
                  <p v-if="post.content" class="content-text">{{ post.content }}</p>
                  <div v-if="post.imageUrls && post.imageUrls.length > 0" class="content-images">
                    <img v-for="(url, idx) in post.imageUrls" :key="idx" :src="url" :alt="`图片 ${idx + 1}`" />
                  </div>
                  <div v-if="post.locationTag" class="content-tag">
                    <AppIcon name="location" />
                    <span>{{ post.locationTag }}</span>
                  </div>
                  <div class="content-stats">
                    <span>{{ post.commentCount }} 条评论</span>
                  </div>
                  <div v-if="post.comments && post.comments.length > 0" class="content-comments">
                    <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                      <strong>{{ comment.authorName }}</strong>
                      <span v-if="comment.isAuthor" class="author-badge">作者</span>
                      <p>{{ comment.content }}</p>
                      <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 路线 -->
            <section v-if="userPublishedContent.routes.length > 0" class="content-section">
              <h4>旅游路线 ({{ userPublishedContent.routes.length }})</h4>
              <div class="content-list">
                <div v-for="route in userPublishedContent.routes" :key="route.id" class="content-item">
                  <div class="content-header">
                    <strong>{{ route.title }}</strong>
                    <span class="content-meta">{{ formatDate(route.createdAt) }}</span>
                  </div>
                  <p v-if="route.summary" class="content-text">{{ route.summary }}</p>
                  <div class="route-info">
                    <span v-if="route.destination">目的地: {{ route.destination }}</span>
                    <span v-if="route.days">天数: {{ route.days }} 天</span>
                    <span v-if="route.budget">预算: {{ route.budget }}</span>
                    <span v-if="route.departure">出发地: {{ route.departure }}</span>
                  </div>
                  <div v-if="route.interests" class="content-tag">
                    <AppIcon name="tag" />
                    <span>{{ route.interests }}</span>
                  </div>
                  <div class="content-stats">
                    <span>{{ route.clickCount }} 次浏览</span>
                    <span>{{ route.applyCount }} 次申请</span>
                    <span>{{ route.commentCount }} 条评论</span>
                  </div>
                  <div v-if="route.points && route.points.length > 0" class="route-points">
                    <strong>行程安排:</strong>
                    <div v-for="point in route.points" :key="`${point.day}-${point.order}`" class="route-point">
                      <span class="point-day">第 {{ point.day }} 天</span>
                      <span class="point-name">{{ point.name }}</span>
                      <p v-if="point.description">{{ point.description }}</p>
                    </div>
                  </div>
                  <div v-if="route.tips && route.tips.length > 0" class="route-tips">
                    <strong>旅行贴士:</strong>
                    <ul>
                      <li v-for="(tip, idx) in route.tips" :key="idx">{{ tip }}</li>
                    </ul>
                  </div>
                  <div v-if="route.comments && route.comments.length > 0" class="content-comments">
                    <div v-for="comment in route.comments" :key="comment.id" class="comment-item">
                      <strong>{{ comment.authorName }}</strong>
                      <span v-if="comment.isAuthor" class="author-badge">作者</span>
                      <p>{{ comment.content }}</p>
                      <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 评论 -->
            <section v-if="userPublishedContent.comments.length > 0" class="content-section">
              <h4>评论 ({{ userPublishedContent.comments.length }})</h4>
              <div class="content-list">
                <div v-for="comment in userPublishedContent.comments" :key="comment.id" class="content-item">
                  <div class="content-header">
                    <span>评论于: {{ comment.postTitle }}</span>
                    <span class="content-meta">{{ formatDate(comment.createdAt) }}</span>
                  </div>
                  <p class="content-text">{{ comment.content }}</p>
                  <span v-if="comment.isRoute" class="content-badge">路线</span>
                </div>
              </div>
            </section>

            <div v-if="userPublishedContent.posts.length === 0 && userPublishedContent.routes.length === 0 && userPublishedContent.comments.length === 0" class="empty-copy">
              该用户暂无发布内容
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, inject, ref } from 'vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { requestJson } from '@/utils/request';

// 注入共享状态
const adminUsers = inject('adminUsers');
const showNotice = inject('showNotice');

// 本地状态
const loadingUsers = ref(false);
const savingUser = ref(false);
const userSearch = ref('');
const userSearchTerm = ref('');
const userForm = ref(createUserForm());
const loadingUserContent = ref(false);
const showContentModal = ref(false);
const userPublishedContent = ref(null);

// 计算属性
const filteredUsers = computed(() => {
  const q = userSearchTerm.value.trim().toLowerCase();
  if (!q) return adminUsers.value;
  return adminUsers.value.filter(
    (item) =>
      (item.displayName || '').toLowerCase().includes(q) ||
      (item.username || '').toLowerCase().includes(q) ||
      (item.email || '').toLowerCase().includes(q),
  );
});

// 工具函数
function createUserForm(item = null) {
  return {
    id: item?.id || null,
    username: item?.username || '',
    displayName: item?.displayName || '',
    email: item?.email || '',
    role: item?.role || 'USER',
    banned: Boolean(item?.banned),
    banReason: item?.banReason || '',
  };
}

// 用户管理方法
async function loadUsers() {
  loadingUsers.value = true;
  try {
    const data = await requestJson('/api/admin/users');
    adminUsers.value = Array.isArray(data) ? data : [];
    if (userForm.value.id) {
      const matched = adminUsers.value.find((item) => item.id === userForm.value.id);
      if (matched) {
        userForm.value = createUserForm(matched);
      }
    }
  } catch (error) {
    showNotice(error.message, 'error');
  } finally {
    loadingUsers.value = false;
  }
}

function applyUser(item) {
  userForm.value = createUserForm(item);
}

async function saveUser() {
  if (!userForm.value.id) {
    return;
  }
  savingUser.value = true;
  try {
    const payload = {
      displayName: userForm.value.displayName,
      email: userForm.value.email,
      role: userForm.value.role,
      banned: userForm.value.banned,
      banReason: userForm.value.banned ? userForm.value.banReason : '',
    };
    const data = await requestJson(`/api/admin/users/${userForm.value.id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    });
    userForm.value = createUserForm(data);
    await loadUsers();
    showNotice('用户信息已更新。', 'success');
  } catch (error) {
    showNotice(error.message, 'error');
  } finally {
    savingUser.value = false;
  }
}

async function openUserPublishedContent() {
  if (!userForm.value.id) {
    return;
  }
  showContentModal.value = true;
  loadingUserContent.value = true;
  try {
    const data = await requestJson(`/api/admin/users/${userForm.value.id}/published-content`);
    userPublishedContent.value = data;
  } catch (error) {
    showNotice(error.message, 'error');
    closeContentModal();
  } finally {
    loadingUserContent.value = false;
  }
}

function closeContentModal() {
  showContentModal.value = false;
  userPublishedContent.value = null;
}

function formatDate(dateString) {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}
</script>

<style>
/* 弹窗样式 - 不使用 scoped，因为 Teleport 渲染到 body */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 2rem;
}

.modal-content {
  background: #ffffff;
  border-radius: 8px;
  max-width: 800px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-content.large {
  max-width: 1000px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #111827;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.content-section {
  margin-bottom: 2rem;
}

.content-section:last-child {
  margin-bottom: 0;
}

.content-section h4 {
  margin: 0 0 1rem 0;
  font-size: 1.1rem;
  color: #111827;
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.content-item {
  padding: 1rem;
  background: #f9fafb;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.content-header strong {
  font-size: 1rem;
  color: #111827;
}

.content-meta {
  font-size: 0.875rem;
  color: #6b7280;
}

.content-text {
  margin: 0.5rem 0;
  color: #374151;
  line-height: 1.6;
}

.content-images {
  display: flex;
  gap: 0.5rem;
  margin: 0.5rem 0;
  flex-wrap: wrap;
}

.content-images img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

.content-tag {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin: 0.5rem 0;
  font-size: 0.875rem;
  color: #6b7280;
}

.content-stats {
  display: flex;
  gap: 1rem;
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.content-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background: #10b981;
  color: white;
  border-radius: 4px;
  font-size: 0.75rem;
  margin-top: 0.5rem;
}

.route-info {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin: 0.5rem 0;
  font-size: 0.875rem;
  color: #374151;
}

.route-points {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.route-points strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #111827;
}

.route-point {
  display: flex;
  gap: 0.5rem;
  margin: 0.5rem 0;
  padding: 0.5rem;
  background: #ffffff;
  border-radius: 4px;
  flex-wrap: wrap;
}

.point-day {
  font-weight: 600;
  color: #10b981;
  min-width: 60px;
}

.point-name {
  font-weight: 500;
  color: #111827;
}

.route-point p {
  margin: 0.25rem 0 0 0;
  color: #6b7280;
  font-size: 0.875rem;
  width: 100%;
}

.route-tips {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.route-tips strong {
  display: block;
  margin-bottom: 0.5rem;
  color: #111827;
}

.route-tips ul {
  margin: 0;
  padding-left: 1.5rem;
}

.route-tips li {
  margin: 0.25rem 0;
  color: #374151;
}

.content-comments {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e5e7eb;
}

.comment-item {
  padding: 0.75rem;
  background: #ffffff;
  border-radius: 4px;
  margin-bottom: 0.5rem;
}

.comment-item:last-child {
  margin-bottom: 0;
}

.comment-item strong {
  color: #111827;
  margin-right: 0.5rem;
}

.author-badge {
  display: inline-block;
  padding: 0.125rem 0.375rem;
  background: #10b981;
  color: white;
  border-radius: 3px;
  font-size: 0.75rem;
  font-weight: normal;
}

.comment-item p {
  margin: 0.5rem 0;
  color: #374151;
}

.comment-time {
  font-size: 0.75rem;
  color: #6b7280;
}

.icon-button {
  background: none;
  border: none;
  padding: 0.5rem;
  cursor: pointer;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.icon-button:hover {
  background: #f3f4f6;
}
</style>

<style scoped>
/* 其他组件样式保持 scoped */
</style>
