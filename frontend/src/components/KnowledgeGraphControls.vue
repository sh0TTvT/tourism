<template>
  <div class="kg-controls-container">
    <div class="kg-controls-panel">
      <!-- 缩放控制 -->
      <div class="kg-control-group">
        <button
          class="kg-control-button"
          title="放大"
          @click="$emit('zoom-in')"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M10 4v12M4 10h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
        <button
          class="kg-control-button"
          title="缩小"
          @click="$emit('zoom-out')"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M4 10h12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
        <button
          class="kg-control-button"
          title="重置视图"
          @click="$emit('reset-view')"
        >
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M4 10a6 6 0 1 1 12 0 6 6 0 0 1-12 0z" stroke="currentColor" stroke-width="2"/>
            <path d="M10 6v4l2 2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <!-- 图例 -->
      <div class="kg-control-divider"></div>
      <button
        class="kg-control-button kg-legend-toggle"
        :class="{ active: showLegend }"
        title="图例"
        @click="toggleLegend"
      >
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
          <rect x="3" y="4" width="4" height="4" rx="1" fill="currentColor"/>
          <rect x="3" y="10" width="4" height="4" rx="1" fill="currentColor"/>
          <path d="M9 6h8M9 12h8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <!-- 图例面板 -->
    <transition name="fade-slide-up">
      <div v-if="showLegend" class="kg-legend-panel">
        <div class="kg-legend-header">
          <span class="kg-legend-title">节点类型</span>
          <button class="kg-legend-close" @click="showLegend = false">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
        <div class="kg-legend-items">
          <div
            v-for="category in categories"
            :key="category.name"
            class="kg-legend-item"
            :class="{ disabled: !category.visible }"
            @click="toggleCategory(category.name)"
          >
            <div
              class="kg-legend-color"
              :style="{ backgroundColor: category.color }"
            ></div>
            <span class="kg-legend-label">{{ category.name }}</span>
            <span class="kg-legend-count">({{ category.count }})</span>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  }
});

const emit = defineEmits(['zoom-in', 'zoom-out', 'reset-view', 'toggle-category']);

const showLegend = ref(false);
const visibleCategories = ref(new Set(['市', '景点', '美食', '酒店']));

// 颜色映射
const categoryColors = {
  '市': '#0F9D7A',
  '景点': '#3B82F6',
  '美食': '#F59E0B',
  '酒店': '#8B5CF6',
  '其他': '#6B7280'
};

// 计算各类别的节点数量
const categories = computed(() => {
  const categoryCounts = {};

  props.nodes.forEach(node => {
    const category = node.category || '其他';
    if (!categoryCounts[category]) {
      categoryCounts[category] = 0;
    }
    categoryCounts[category]++;
  });

  return Object.entries(categoryCounts).map(([name, count]) => ({
    name,
    color: categoryColors[name] || categoryColors['其他'],
    count,
    visible: visibleCategories.value.has(name)
  }));
});

// 切换图例显示
const toggleLegend = () => {
  showLegend.value = !showLegend.value;
};

// 切换类别显示
const toggleCategory = (categoryName) => {
  if (visibleCategories.value.has(categoryName)) {
    visibleCategories.value.delete(categoryName);
  } else {
    visibleCategories.value.add(categoryName);
  }
  emit('toggle-category', categoryName, visibleCategories.value.has(categoryName));
};
</script>

<style scoped>
.kg-controls-container {
  position: absolute;
  bottom: 20px;
  left: 20px;
  z-index: 100;
}

.kg-controls-panel {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 8px;
}

.kg-control-group {
  display: flex;
  gap: 4px;
}

.kg-control-button {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #6B7280;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.kg-control-button:hover {
  background: #F3F4F6;
  color: #1F2937;
}

.kg-control-button.active {
  background: #0F9D7A;
  color: white;
}

.kg-control-divider {
  width: 1px;
  height: 24px;
  background: #E5E7EB;
}

.kg-legend-toggle {
  /* 特殊样式 */
}

.kg-legend-panel {
  position: absolute;
  bottom: 60px;
  left: 0;
  width: 240px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.kg-legend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E7EB;
}

.kg-legend-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2937;
}

.kg-legend-close {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #9CA3AF;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.kg-legend-close:hover {
  background: #F3F4F6;
  color: #6B7280;
}

.kg-legend-items {
  padding: 8px;
}

.kg-legend-item {
  display: flex;
  align-items: center;
  padding: 8px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.kg-legend-item:hover {
  background: #F9FAFB;
}

.kg-legend-item.disabled {
  opacity: 0.4;
}

.kg-legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  margin-right: 12px;
  flex-shrink: 0;
}

.kg-legend-label {
  flex: 1;
  font-size: 14px;
  color: #1F2937;
}

.kg-legend-count {
  font-size: 12px;
  color: #9CA3AF;
}

.fade-slide-up-enter-active,
.fade-slide-up-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-up-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
