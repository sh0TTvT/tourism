<template>
  <transition name="slide">
    <div v-if="visible && node" class="node-detail-panel">
      <!-- 面板头部 -->
      <div class="panel-header">
        <h3 class="panel-title">{{ node.name }}</h3>
        <button @click="$emit('close')" class="close-btn">×</button>
      </div>

      <!-- 面板内容 -->
      <div class="panel-content">
        <!-- 基本信息 -->
        <div class="info-section">
          <div class="info-label">类型</div>
          <div class="info-value">
            <span :class="['type-badge', node.category || node.type]">
              {{ getTypeLabel(node.category || node.type) }}
            </span>
          </div>
        </div>

        <div v-if="node.description" class="info-section">
          <div class="info-label">描述</div>
          <div class="info-value">{{ node.description }}</div>
        </div>

        <!-- 关系列表 -->
        <div v-if="relationships.length > 0" class="info-section">
          <div class="info-label">关系 ({{ relationships.length }})</div>
          <div class="relationships-list">
            <div
              v-for="rel in relationships"
              :key="rel.id"
              class="relationship-item"
              @click="$emit('navigate-to-node', rel.targetNodeId)"
            >
              <template v-if="rel.isOutgoing">
                <span class="rel-predicate">{{ rel.predicate }}</span>
                <span class="rel-arrow">→</span>
                <span class="rel-target">{{ rel.targetNodeName }}</span>
              </template>
              <template v-else>
                <span class="rel-predicate">{{ rel.predicate }}</span>
                <span class="rel-arrow">←</span>
                <span class="rel-target">{{ rel.targetNodeName }}</span>
              </template>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div v-if="node.childCount" class="info-section">
          <div class="info-label">子节点数量</div>
          <div class="info-value">{{ node.childCount }}</div>
        </div>
      </div>

      <!-- 面板底部操作 -->
      <div class="panel-footer">
        <button @click="$emit('edit-node', node)" class="action-btn primary">
          编辑节点
        </button>
        <button @click="$emit('expand-node', node)" class="action-btn">
          展开节点
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  node: {
    type: Object,
    default: null
  },
  relationships: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'edit-node', 'expand-node', 'navigate-to-node'])

function getTypeLabel(type) {
  const labels = {
    city: '城市',
    attraction: '景点',
    restaurant: '餐厅',
    hotel: '酒店'
  }
  return labels[type] || type || '未知'
}
</script>

<style scoped>
.node-detail-panel {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 400px;
  background: white;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 1000;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.close-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 28px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.info-section {
  margin-bottom: 20px;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}

.info-value {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.6;
}

.type-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.type-badge.city,
.type-badge.国家,
.type-badge.省份,
.type-badge.城市,
.type-badge.区县 {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.type-badge.attraction,
.type-badge.景点 {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.type-badge.restaurant,
.type-badge.美食,
.type-badge.小吃,
.type-badge.点心,
.type-badge.主菜,
.type-badge.主食,
.type-badge.汤羹,
.type-badge.饮品,
.type-badge.早餐,
.type-badge.特产 {
  background: rgba(251, 146, 60, 0.1);
  color: #fb923c;
}

.type-badge.hotel,
.type-badge.酒店 {
  background: rgba(236, 72, 153, 0.1);
  color: #ec4899;
}

.type-badge.名人 {
  background: rgba(20, 184, 166, 0.1);
  color: #14b8a6;
}

.type-badge.文化 {
  background: rgba(234, 179, 8, 0.1);
  color: #eab308;
}

.relationships-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.relationship-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.relationship-item:hover {
  background: #f3f4f6;
}

.rel-predicate {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.rel-arrow {
  color: #9ca3af;
}

.rel-target {
  font-size: 14px;
  color: #3b82f6;
  font-weight: 500;
}

.panel-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

.action-btn {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
}

.action-btn.primary {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.action-btn.primary:hover {
  background: #2563eb;
  border-color: #2563eb;
}

/* 滑入动画 */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
