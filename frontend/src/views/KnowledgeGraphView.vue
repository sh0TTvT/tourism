<template>
  <div class="kg-view">
    <!-- 左侧：图谱画布 -->
    <div class="kg-canvas-area" :class="{ 'full-width': !showEditor }">
      <KnowledgeGraphSearch
        :nodes="allNodes"
        @search="handleSearch"
        @select="handleSearchSelect"
      />

      <KnowledgeGraphCanvas
        ref="canvasRef"
        :nodes="displayNodes"
        :edges="displayEdges"
        :selected-node-id="selectedNodeId"
        :view-mode="viewMode"
        @node-click="handleNodeClick"
        @node-hover="handleNodeHover"
        @canvas-click="handleCanvasClick"
      />

      <KnowledgeGraphControls
        :nodes="displayNodes"
        @zoom-in="handleZoomIn"
        @zoom-out="handleZoomOut"
        @reset-view="handleResetView"
        @toggle-category="handleToggleCategory"
      />

      <!-- 加载提示 -->
      <div v-if="loading" class="kg-loading-overlay">
        <div class="kg-loading-spinner"></div>
        <span>加载中...</span>
      </div>
    </div>

    <!-- 右侧：编辑面板 -->
    <transition name="slide-left">
      <div v-if="showEditor" class="kg-editor-area">
        <div class="kg-editor-header">
          <h3>{{ editorTitle }}</h3>
          <button class="kg-editor-close" @click="closeEditor">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
              <path d="M15 5L5 15M5 5l10 10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="kg-editor-content">
          <!-- 节点编辑器 -->
          <div v-if="editorMode === 'node' && selectedNode" class="kg-node-editor">
            <div class="kg-form-group">
              <label>节点名称</label>
              <input
                v-model="nodeForm.name"
                type="text"
                class="kg-input"
                placeholder="输入节点名称"
              />
            </div>

            <div class="kg-form-group">
              <label>类别</label>
              <input
                v-model="nodeForm.category"
                type="text"
                class="kg-input"
                placeholder="输入类别"
              />
            </div>

            <div class="kg-form-group">
              <label>描述</label>
              <textarea
                v-model="nodeForm.description"
                class="kg-textarea"
                rows="4"
                placeholder="输入描述"
              ></textarea>
            </div>

            <div class="kg-form-group">
              <label>别名（逗号分隔）</label>
              <input
                v-model="aliasesText"
                type="text"
                class="kg-input"
                placeholder="输入别名，用逗号分隔"
              />
            </div>

            <div class="kg-form-group">
              <label>标签（逗号分隔）</label>
              <input
                v-model="tagsText"
                type="text"
                class="kg-input"
                placeholder="输入标签，用逗号分隔"
              />
            </div>

            <div class="kg-form-actions">
              <button class="kg-button kg-button-primary" @click="saveNode">
                保存
              </button>
              <button class="kg-button kg-button-danger" @click="deleteNode">
                删除
              </button>
            </div>
          </div>

          <!-- 关系编辑器 -->
          <div v-if="editorMode === 'relationship' && selectedRelationship" class="kg-relationship-editor">
            <div class="kg-form-group">
              <label>关系类型</label>
              <input
                v-model="relationshipForm.predicate"
                type="text"
                class="kg-input"
                placeholder="输入关系类型"
              />
            </div>

            <div class="kg-form-group">
              <label>描述</label>
              <textarea
                v-model="relationshipForm.description"
                class="kg-textarea"
                rows="4"
                placeholder="输入描述"
              ></textarea>
            </div>

            <div class="kg-form-group">
              <label>权重</label>
              <input
                v-model.number="relationshipForm.weight"
                type="number"
                class="kg-input"
                step="0.1"
                placeholder="输入权重"
              />
            </div>

            <div class="kg-form-actions">
              <button class="kg-button kg-button-primary" @click="saveRelationship">
                保存
              </button>
              <button class="kg-button kg-button-danger" @click="deleteRelationship">
                删除
              </button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!selectedNode && !selectedRelationship" class="kg-editor-empty">
            <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="32" r="30" stroke="#E5E7EB" stroke-width="2"/>
              <path d="M32 20v24M20 32h24" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <p>点击节点或关系进行编辑</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue';
import KnowledgeGraphCanvas from './KnowledgeGraphCanvas.vue';
import KnowledgeGraphSearch from './KnowledgeGraphSearch.vue';
import KnowledgeGraphControls from './KnowledgeGraphControls.vue';

const canvasRef = ref(null);
const loading = ref(false);
const showEditor = ref(true);

// 视图状态
const viewMode = ref('overview'); // 'overview' | 'expanded' | 'focused'
const expandedCityId = ref(null);

// 数据
const allCityNodes = ref([]);
const expandedNodes = ref([]);
const allEdges = ref([]);
const nodeCache = reactive(new Map());
const allNodes = ref([]);

// 选中状态
const selectedNodeId = ref(null);
const selectedNode = ref(null);
const selectedRelationship = ref(null);
const editorMode = ref('node'); // 'node' | 'relationship'

// 表单数据
const nodeForm = reactive({
  name: '',
  category: '',
  description: '',
  aliases: [],
  tags: []
});

const relationshipForm = reactive({
  predicate: '',
  description: '',
  weight: 1.0
});

const aliasesText = ref('');
const tagsText = ref('');

// 计算属性
const displayNodes = computed(() => {
  if (viewMode.value === 'overview') {
    return allCityNodes.value;
  } else if (viewMode.value === 'expanded') {
    return [
      ...allCityNodes.value.map(n => ({
        ...n,
        dimmed: n.id !== expandedCityId.value
      })),
      ...expandedNodes.value
    ];
  }
  return [];
});

const displayEdges = computed(() => {
  return allEdges.value;
});

const editorTitle = computed(() => {
  if (editorMode.value === 'node' && selectedNode.value) {
    return `编辑节点：${selectedNode.value.name}`;
  }
  if (editorMode.value === 'relationship' && selectedRelationship.value) {
    return `编辑关系：${selectedRelationship.value.predicate}`;
  }
  return '编辑器';
});

// API 调用
const fetchCityNodes = async () => {
  loading.value = true;
  try {
    const response = await fetch('/api/kg/cities', {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    if (!response.ok) throw new Error('获取市节点失败');
    const data = await response.json();
    allCityNodes.value = data;
    allNodes.value = [...data];
  } catch (error) {
    console.error('获取市节点失败:', error);
    alert('获取市节点失败');
  } finally {
    loading.value = false;
  }
};

const expandCity = async (cityId) => {
  // 检查缓存
  if (nodeCache.has(cityId)) {
    const cached = nodeCache.get(cityId);
    expandedNodes.value = cached.nodes;
    allEdges.value = cached.edges;
    expandedCityId.value = cityId;
    viewMode.value = 'expanded';
    return;
  }

  loading.value = true;
  try {
    const response = await fetch(`/api/kg/cities/${cityId}/expand`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    if (!response.ok) throw new Error('展开市节点失败');
    const data = await response.json();

    // 缓存数据
    nodeCache.set(cityId, {
      nodes: data.nodes,
      edges: data.edges
    });

    expandedNodes.value = data.nodes;
    allEdges.value = data.edges;
    expandedCityId.value = cityId;
    viewMode.value = 'expanded';
    allNodes.value = [...allCityNodes.value, ...data.nodes];
  } catch (error) {
    console.error('展开市节点失败:', error);
    alert('展开市节点失败');
  } finally {
    loading.value = false;
  }
};

const updateNode = async (nodeId, data) => {
  try {
    const response = await fetch(`/api/kg/nodes/${nodeId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(data)
    });
    if (!response.ok) throw new Error('更新节点失败');
    return await response.json();
  } catch (error) {
    console.error('更新节点失败:', error);
    throw error;
  }
};

const removeNode = async (nodeId) => {
  try {
    const response = await fetch(`/api/kg/nodes/${nodeId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    });
    if (!response.ok) throw new Error('删除节点失败');
  } catch (error) {
    console.error('删除节点失败:', error);
    throw error;
  }
};

// 事件处理
const handleNodeClick = (node) => {
  if (node.category === '市') {
    if (viewMode.value === 'overview') {
      expandCity(node.id);
    } else if (viewMode.value === 'expanded' && node.id === expandedCityId.value) {
      // 点击已展开的市节点，收起
      collapseCity();
    } else if (viewMode.value === 'expanded' && node.id !== expandedCityId.value) {
      // 点击其他市节点，切换展开
      expandCity(node.id);
    }
  } else {
    // 点击子节点，显示编辑器
    selectNode(node);
  }
};

const handleNodeHover = (node) => {
  // 可以添加悬停效果
};

const handleCanvasClick = () => {
  if (viewMode.value === 'expanded') {
    collapseCity();
  }
};

const collapseCity = () => {
  expandedNodes.value = [];
  allEdges.value = [];
  expandedCityId.value = null;
  viewMode.value = 'overview';
  allNodes.value = [...allCityNodes.value];
  selectedNodeId.value = null;
  selectedNode.value = null;
};

const selectNode = (node) => {
  selectedNodeId.value = node.id;
  selectedNode.value = node;
  selectedRelationship.value = null;
  editorMode.value = 'node';
  showEditor.value = true;

  // 填充表单
  nodeForm.name = node.name || '';
  nodeForm.category = node.category || '';
  nodeForm.description = node.description || '';
  nodeForm.aliases = node.aliases || [];
  nodeForm.tags = node.tags || [];
  aliasesText.value = (node.aliases || []).join(', ');
  tagsText.value = (node.tags || []).join(', ');
};

const handleSearch = (query) => {
  // 搜索逻辑已在 KnowledgeGraphSearch 组件中处理
};

const handleSearchSelect = (node) => {
  if (node.category === '市') {
    expandCity(node.id);
  } else {
    // 需要先找到该节点所属的市，然后展开
    // 这里简化处理，直接选中节点
    selectNode(node);
  }
};

const handleZoomIn = () => {
  canvasRef.value?.zoomIn();
};

const handleZoomOut = () => {
  canvasRef.value?.zoomOut();
};

const handleResetView = () => {
  canvasRef.value?.resetView();
  collapseCity();
};

const handleToggleCategory = (category, visible) => {
  // 可以实现类别过滤逻辑
};

const closeEditor = () => {
  showEditor.value = false;
};

const saveNode = async () => {
  if (!selectedNode.value) return;

  const data = {
    name: nodeForm.name,
    category: nodeForm.category,
    description: nodeForm.description,
    aliases: aliasesText.value.split(',').map(s => s.trim()).filter(s => s),
    tags: tagsText.value.split(',').map(s => s.trim()).filter(s => s)
  };

  try {
    const updated = await updateNode(selectedNode.value.id, data);
    // 更新本地数据
    const index = expandedNodes.value.findIndex(n => n.id === updated.id);
    if (index !== -1) {
      expandedNodes.value[index] = updated;
    }
    // 清除缓存
    if (expandedCityId.value) {
      nodeCache.delete(expandedCityId.value);
    }
    alert('保存成功');
    selectNode(updated);
  } catch (error) {
    alert('保存失败');
  }
};

const deleteNode = async () => {
  if (!selectedNode.value) return;
  if (!confirm(`确定要删除节点"${selectedNode.value.name}"吗？`)) return;

  try {
    await removeNode(selectedNode.value.id);
    // 更新本地数据
    expandedNodes.value = expandedNodes.value.filter(n => n.id !== selectedNode.value.id);
    // 清除缓存
    if (expandedCityId.value) {
      nodeCache.delete(expandedCityId.value);
    }
    alert('删除成功');
    selectedNode.value = null;
    selectedNodeId.value = null;
  } catch (error) {
    alert('删除失败');
  }
};

const saveRelationship = async () => {
  // 实现关系保存逻辑
};

const deleteRelationship = async () => {
  // 实现关系删除逻辑
};

// 生命周期
onMounted(() => {
  fetchCityNodes();
});
</script>

<style scoped>
.kg-view {
  display: flex;
  width: 100%;
  height: 100vh;
  background: #f9fafb;
}

.kg-canvas-area {
  flex: 1;
  position: relative;
  transition: all 0.3s ease;
}

.kg-canvas-area.full-width {
  flex: 1;
}

.kg-loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  z-index: 1000;
}

.kg-loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #E5E7EB;
  border-top-color: #0F9D7A;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.kg-editor-area {
  width: 400px;
  background: white;
  border-left: 1px solid #E5E7EB;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.kg-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid #E5E7EB;
}

.kg-editor-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

.kg-editor-close {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: #9CA3AF;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.kg-editor-close:hover {
  background: #F3F4F6;
  color: #6B7280;
}

.kg-editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.kg-form-group {
  margin-bottom: 20px;
}

.kg-form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.kg-input,
.kg-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #D1D5DB;
  border-radius: 8px;
  font-size: 14px;
  color: #1F2937;
  transition: all 0.2s ease;
}

.kg-input:focus,
.kg-textarea:focus {
  outline: none;
  border-color: #0F9D7A;
  box-shadow: 0 0 0 3px rgba(15, 157, 122, 0.1);
}

.kg-textarea {
  resize: vertical;
  font-family: inherit;
}

.kg-form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.kg-button {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.kg-button-primary {
  background: #0F9D7A;
  color: white;
}

.kg-button-primary:hover {
  background: #0D8A6C;
}

.kg-button-danger {
  background: #EF4444;
  color: white;
}

.kg-button-danger:hover {
  background: #DC2626;
}

.kg-editor-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9CA3AF;
  text-align: center;
}

.kg-editor-empty p {
  margin-top: 16px;
  font-size: 14px;
}

.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.3s ease;
}

.slide-left-enter-from {
  transform: translateX(100%);
}

.slide-left-leave-to {
  transform: translateX(100%);
}
</style>
