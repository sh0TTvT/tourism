<template>
  <div class="graph-toolbar">
    <!-- 搜索框 -->
    <div class="toolbar-section search-section" ref="searchRef">
      <div class="search-wrapper">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索节点..."
          class="search-input"
          @keydown.enter="handleSearch"
          @keydown.esc="closeDropdown"
        />
        <button
          v-if="searchQuery"
          @click="clearSearch"
          class="clear-btn"
          title="清除搜索"
        >×</button>
      </div>

      <!-- 搜索结果下拉 -->
      <div v-if="showDropdown" class="search-dropdown">
        <div v-if="searchLoading" class="dropdown-status">搜索中...</div>
        <div v-else-if="searchQuery && searchResults.length === 0" class="dropdown-status">
          未找到相关节点
        </div>
        <template v-else>
          <div
            v-for="node in searchResults"
            :key="node.id"
            class="dropdown-item"
            @click="handleSelectNode(node)"
          >
            <span class="item-name">{{ node.name }}</span>
            <span v-if="node.category" class="item-category">{{ node.category }}</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 缩放控制 -->
    <div class="toolbar-section">
      <button @click="$emit('zoom-in')" class="toolbar-btn" title="放大">
        <span class="icon">+</span>
      </button>
      <button @click="$emit('zoom-out')" class="toolbar-btn" title="缩小">
        <span class="icon">−</span>
      </button>
      <button @click="$emit('zoom-reset')" class="toolbar-btn" title="重置缩放">
        <span class="icon">⊙</span>
      </button>
    </div>

    <!-- 布局切换 -->
    <div class="toolbar-section">
      <select v-model="selectedLayout" @change="handleLayoutChange" class="layout-select">
        <option value="force">力导向</option>
        <option value="hierarchical">层次布局</option>
        <option value="circular">环形布局</option>
      </select>
    </div>

    <!-- 返回概览 -->
    <div class="toolbar-section">
      <button @click="$emit('back-to-overview')" class="toolbar-btn" title="返回概览">
        <span class="icon">⌂</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { requestJson } from '@/utils/request'

const emit = defineEmits([
  'navigate-to-node',
  'zoom-in',
  'zoom-out',
  'zoom-reset',
  'layout-change',
  'back-to-overview'
])

const searchQuery = ref('')
const selectedLayout = ref('force')
const searchResults = ref([])
const searchLoading = ref(false)
const showDropdown = ref(false)
const searchRef = ref(null)

async function handleSearch() {
  const keyword = searchQuery.value.trim()
  if (!keyword) {
    searchResults.value = []
    showDropdown.value = false
    return
  }

  searchLoading.value = true
  showDropdown.value = true
  try {
    const results = await requestJson(`/api/kg/nodes?keyword=${encodeURIComponent(keyword)}&limit=50`)
    const list = Array.isArray(results) ? results : []

    // 按相关性排序：名称精确匹配 > 名称包含 > 其他匹配
    const kw = keyword.toLowerCase()
    list.sort((a, b) => {
      const aName = (a.name || '').toLowerCase()
      const bName = (b.name || '').toLowerCase()
      const aExact = aName === kw
      const bExact = bName === kw
      if (aExact !== bExact) return aExact ? -1 : 1
      const aContains = aName.includes(kw)
      const bContains = bName.includes(kw)
      if (aContains !== bContains) return aContains ? -1 : 1
      return 0
    })

    searchResults.value = list

    // 自动导航到最佳匹配
    if (list.length > 0) {
      emit('navigate-to-node', list[0].id)
    }
  } catch (err) {
    console.error('搜索节点失败:', err)
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

function handleSelectNode(node) {
  emit('navigate-to-node', node.id)
  closeDropdown()
}

function clearSearch() {
  searchQuery.value = ''
  searchResults.value = []
  showDropdown.value = false
}

function closeDropdown() {
  showDropdown.value = false
}

function handleClickOutside(e) {
  if (searchRef.value && !searchRef.value.contains(e.target)) {
    closeDropdown()
  }
}

function handleLayoutChange() {
  emit('layout-change', selectedLayout.value)
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

defineExpose({ clearSearch })
</script>

<style scoped>
.graph-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.toolbar-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-section {
  position: relative;
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-input {
  width: 240px;
  padding: 8px 32px 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.clear-btn {
  position: absolute;
  right: 4px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  font-size: 16px;
  color: #9ca3af;
  cursor: pointer;
}

.clear-btn:hover {
  background: #f3f4f6;
  color: #4b5563;
}

.search-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  width: 320px;
  max-height: 360px;
  overflow-y: auto;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  z-index: 100;
}

.dropdown-status {
  padding: 12px 16px;
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-item:hover {
  background: #f3f4f6;
}

.item-name {
  font-size: 14px;
  color: #1f2937;
  font-weight: 500;
}

.item-category {
  font-size: 12px;
  color: #6b7280;
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
}

.toolbar-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.toolbar-btn:active {
  transform: scale(0.95);
}

.toolbar-btn .icon {
  font-size: 18px;
  font-weight: 600;
  color: #4b5563;
}

.layout-select {
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.layout-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}
</style>
