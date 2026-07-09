<template>
  <div class="kg-search-container">
    <div class="kg-search-box">
      <svg class="kg-search-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
        <path d="M9 17A8 8 0 1 0 9 1a8 8 0 0 0 0 16zM18 18l-4.35-4.35" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <input
        v-model="searchQuery"
        type="text"
        class="kg-search-input"
        placeholder="搜索节点名称、类别、标签..."
        maxlength="100"
        @input="handleSearch"
        @keydown.enter="handleEnter"
        @focus="showResults = true"
      />
      <button
        v-if="searchQuery"
        class="kg-search-clear"
        @click="clearSearch"
      >
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
          <path d="M12 4L4 12M4 4l8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <transition name="fade-slide">
      <div
        v-if="showResults && filteredResults.length > 0"
        class="kg-search-results"
      >
        <div
          v-for="result in filteredResults"
          :key="result.id"
          class="kg-search-result-item"
          @click="selectResult(result)"
        >
          <div class="kg-result-icon" :style="{ backgroundColor: getCategoryColor(result.category) }">
            {{ result.category || '节' }}
          </div>
          <div class="kg-result-content">
            <div class="kg-result-name">{{ result.name }}</div>
            <div class="kg-result-meta">
              <span v-if="result.category" class="kg-result-category">{{ result.category }}</span>
              <span v-if="result.tags && result.tags.length > 0" class="kg-result-tags">
                {{ result.tags.join(', ') }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </transition>

    <div
      v-if="showResults && searchQuery && filteredResults.length === 0"
      class="kg-search-empty"
    >
      <span>未找到匹配的节点</span>
    </div>
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

const emit = defineEmits(['search', 'select']);

const searchQuery = ref('');
const showResults = ref(false);
let searchTimeout = null;

// 颜色映射
const categoryColors = {
  '市': '#0F9D7A',
  '景点': '#3B82F6',
  '美食': '#F59E0B',
  '酒店': '#8B5CF6',
  '默认': '#6B7280'
};

// 获取类别颜色
const getCategoryColor = (category) => {
  return categoryColors[category] || categoryColors['默认'];
};

// 过滤搜索结果
const filteredResults = computed(() => {
  if (!searchQuery.value.trim()) {
    return [];
  }

  const query = searchQuery.value.toLowerCase().trim();
  return props.nodes.filter(node => {
    // 搜索名称
    if (node.name && node.name.toLowerCase().includes(query)) {
      return true;
    }
    // 搜索类别
    if (node.category && node.category.toLowerCase().includes(query)) {
      return true;
    }
    // 搜索别名
    if (node.aliases && node.aliases.some(alias => alias.toLowerCase().includes(query))) {
      return true;
    }
    // 搜索标签
    if (node.tags && node.tags.some(tag => tag.toLowerCase().includes(query))) {
      return true;
    }
    return false;
  }).slice(0, 10); // 限制最多显示 10 个结果
});

// 处理搜索输入（防抖）
const handleSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout);
  }
  searchTimeout = setTimeout(() => {
    emit('search', searchQuery.value);
  }, 300);
};

// 处理回车键
const handleEnter = () => {
  if (filteredResults.value.length > 0) {
    selectResult(filteredResults.value[0]);
  }
};

// 选择搜索结果
const selectResult = (result) => {
  emit('select', result);
  showResults.value = false;
};

// 清除搜索
const clearSearch = () => {
  searchQuery.value = '';
  showResults.value = false;
  emit('search', '');
};

// 点击外部关闭结果
const handleClickOutside = (event) => {
  const container = event.target.closest('.kg-search-container');
  if (!container) {
    showResults.value = false;
  }
};

// 监听点击外部
watch(showResults, (newVal) => {
  if (newVal) {
    document.addEventListener('click', handleClickOutside);
  } else {
    document.removeEventListener('click', handleClickOutside);
  }
});
</script>

<style scoped>
.kg-search-container {
  position: absolute;
  top: 20px;
  left: 20px;
  z-index: 100;
  width: 360px;
}

.kg-search-box {
  position: relative;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 12px 16px;
  transition: all 0.3s ease;
}

.kg-search-box:focus-within {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
  background: rgba(255, 255, 255, 1);
}

.kg-search-icon {
  color: #9CA3AF;
  flex-shrink: 0;
  margin-right: 12px;
}

.kg-search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #1F2937;
}

.kg-search-input::placeholder {
  color: #9CA3AF;
}

.kg-search-clear {
  flex-shrink: 0;
  padding: 4px;
  border: none;
  background: transparent;
  color: #9CA3AF;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.kg-search-clear:hover {
  background: #F3F4F6;
  color: #6B7280;
}

.kg-search-results {
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  max-height: 400px;
  overflow-y: auto;
}

.kg-search-result-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-bottom: 1px solid #F3F4F6;
}

.kg-search-result-item:last-child {
  border-bottom: none;
}

.kg-search-result-item:hover {
  background: #F9FAFB;
}

.kg-result-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
  margin-right: 12px;
}

.kg-result-content {
  flex: 1;
  min-width: 0;
}

.kg-result-name {
  font-size: 14px;
  font-weight: 500;
  color: #1F2937;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6B7280;
}

.kg-result-category {
  padding: 2px 8px;
  background: #F3F4F6;
  border-radius: 4px;
}

.kg-result-tags {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kg-search-empty {
  margin-top: 8px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  color: #9CA3AF;
  font-size: 14px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
