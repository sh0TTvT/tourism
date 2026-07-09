<template>
  <section class="overview-grid">
    <article class="surface hero-card overview-card overview-hero-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Overview</p>
          <h3>后台总览</h3>
        </div>
      </div>
      <div class="overview-card-scroll">
        <p class="hero-copy">
          当前后台已拆分为独立前端工程。这里汇总用户状态、模型与外部服务可用性，以及知识图谱维护入口。
        </p>
        <div class="stats-grid">
          <article class="stat-card">
            <span>封禁用户</span>
            <strong>{{ bannedUsersCount }}</strong>
          </article>
          <article class="stat-card">
            <span>默认模型</span>
            <strong>{{ defaultModelsCount }}</strong>
          </article>
          <article class="stat-card">
            <span>可用模型</span>
            <strong>{{ availableModelsCount }}</strong>
          </article>
          <article class="stat-card">
            <span>可用服务</span>
            <strong>{{ availableServicesCount }}</strong>
          </article>
          <article class="stat-card">
            <span>关系数量</span>
            <strong>{{ kgRelationships.length }}</strong>
          </article>
        </div>
      </div>
    </article>

    <article class="surface overview-card overview-users-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Users</p>
          <h3>重点用户</h3>
        </div>
        <button type="button" class="text-button" @click="goToSection('users')">
          管理用户
        </button>
      </div>
      <div class="overview-card-scroll">
        <div v-if="problemUsers.length" class="simple-list">
          <button
            v-for="item in problemUsers"
            :key="item.id"
            type="button"
            class="simple-row"
            @click="openUserSection(item)"
          >
            <div>
              <strong>{{ item.displayName || item.username }}</strong>
              <span>{{ item.email }}</span>
            </div>
            <em>{{ item.banned ? "已封禁" : item.role }}</em>
          </button>
        </div>
        <p v-else class="empty-copy">当前没有重点用户需要处理。</p>
      </div>
    </article>

    <article class="surface overview-card overview-models-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Models</p>
          <h3>模型状态</h3>
        </div>
        <button type="button" class="text-button" @click="goToSection('models')">
          管理模型
        </button>
      </div>
      <div class="overview-card-scroll">
        <div v-if="adminModels.length" class="simple-list">
          <div
            v-for="item in adminModels.slice(0, 5)"
            :key="item.id"
            class="simple-row static"
          >
            <div>
              <strong>{{ item.displayName }}</strong>
              <span>{{ item.provider }} · {{ item.modelId }}</span>
            </div>
            <em>{{ item.available ? "可用" : "不可用" }}</em>
          </div>
        </div>
        <p v-else class="empty-copy">暂无模型配置。</p>
      </div>
    </article>

    <article class="surface overview-card overview-services-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Services</p>
          <h3>服务状态</h3>
        </div>
        <button type="button" class="text-button" @click="goToSection('services')">
          管理服务
        </button>
      </div>
      <div class="overview-card-scroll">
        <div v-if="adminServices.length" class="simple-list">
          <div
            v-for="item in adminServices"
            :key="item.serviceKey"
            class="simple-row static"
          >
            <div>
              <strong>{{ item.displayName }}</strong>
              <span>{{ item.statusMessage || item.description }}</span>
            </div>
            <em>{{ item.available ? "可用" : item.enabled ? "异常" : "已关闭" }}</em>
          </div>
        </div>
        <p v-else class="empty-copy">暂无服务配置。</p>
      </div>
    </article>

    <article class="surface overview-card overview-graph-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">Graph</p>
          <h3>图谱最近节点</h3>
        </div>
        <button type="button" class="text-button" @click="goToSection('knowledge')">
          维护图谱
        </button>
      </div>
      <div class="overview-card-scroll">
        <div v-if="kgNodes.length" class="simple-list">
          <button
            v-for="item in kgNodes.slice(0, 5)"
            :key="item.id"
            type="button"
            class="simple-row"
            @click="openKnowledgeSection(item)"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.category || "未分类" }}</span>
            </div>
            <em>#{{ item.id }}</em>
          </button>
        </div>
        <p v-else class="empty-copy">暂无图谱节点。</p>
      </div>
    </article>
  </section>
</template>

<script setup>
import { inject } from "vue";

// 从 AdminApp 注入共享状态和方法
const adminUsers = inject("adminUsers");
const adminModels = inject("adminModels");
const adminServices = inject("adminServices");
const kgNodes = inject("kgNodes");
const kgRelationships = inject("kgRelationships");
const bannedUsersCount = inject("bannedUsersCount");
const availableModelsCount = inject("availableModelsCount");
const availableServicesCount = inject("availableServicesCount");
const defaultModelsCount = inject("defaultModelsCount");
const problemUsers = inject("problemUsers");
const goToSection = inject("goToSection");
const openUserSection = inject("openUserSection");
const openKnowledgeSection = inject("openKnowledgeSection");
</script>

<style scoped>
/* 使用全局定义的 grid 布局，不覆盖 */
.overview-grid {
  width: 100%;
  height: 100%;
}

/* 卡片内部滚动区域 */
.overview-card-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

/* 简单列表样式 */
.simple-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.simple-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: rgba(248, 250, 252, 0.92);
  border-radius: 12px;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.2s;
}

.simple-row:hover {
  background: rgba(15, 157, 122, 0.08);
}

.simple-row.static {
  cursor: default;
  pointer-events: none;
}

.simple-row.static:hover {
  background: rgba(248, 250, 252, 0.92);
}

.simple-row div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.simple-row strong {
  font-size: 0.9375rem;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.simple-row span {
  font-size: 0.8125rem;
  color: var(--text-soft);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.simple-row em {
  font-style: normal;
  font-size: 0.8125rem;
  color: var(--text-muted);
  flex-shrink: 0;
}

.empty-copy {
  color: var(--text-soft);
  text-align: center;
  padding: 2rem 1rem;
  margin: 0;
}
</style>
