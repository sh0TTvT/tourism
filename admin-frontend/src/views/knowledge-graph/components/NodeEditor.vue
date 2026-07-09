<template>
  <div v-if="visible" class="node-editor-overlay" @click.self="handleClose">
    <div class="node-editor">
      <div class="editor-header">
        <h3 class="editor-title">编辑节点</h3>
        <button @click="handleClose" class="close-btn">×</button>
      </div>

      <div class="editor-content">
        <div class="form-group">
          <label class="form-label">节点名称</label>
          <input
            v-model="formData.name"
            type="text"
            class="form-input"
            placeholder="输入节点名称"
          />
        </div>

        <div class="form-group">
          <label class="form-label">类型</label>
          <select v-model="formData.category" class="form-select">
            <option value="城市">城市</option>
            <option value="景点">景点</option>
            <option value="餐厅">餐厅</option>
            <option value="酒店">酒店</option>
            <option value="美食">美食</option>
            <option value="特产">特产</option>
            <option value="名人">名人</option>
            <option value="文化">文化</option>
            <option value="饮品">饮品</option>
            <option value="主菜">主菜</option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">描述</label>
          <textarea
            v-model="formData.description"
            class="form-textarea"
            rows="4"
            placeholder="输入节点描述"
          ></textarea>
        </div>

        <!-- 关系管理 -->
        <div class="form-group">
          <label class="form-label">关系管理</label>

          <!-- 筛选控件 -->
          <div class="rel-filter-row">
            <select v-model="filterPredicate" class="form-select rel-filter-select">
              <option value="">全部类型</option>
              <option v-for="p in predicateOptions" :key="p" :value="p">{{ p }}</option>
            </select>
            <input
              v-model="filterKeywordInput"
              type="text"
              class="form-input rel-filter-input"
              placeholder="按节点名称筛选（回车确认）"
              @keydown.enter="filterKeyword = filterKeywordInput.trim()"
            />
            <button v-if="filterKeyword" @click="filterKeyword = ''; filterKeywordInput = ''" class="rel-filter-clear" title="清除筛选">×</button>
          </div>

          <div v-if="relLoading" class="rel-status">加载中...</div>
          <div v-else-if="relationships.length === 0" class="rel-status">暂无关系</div>
          <div v-else-if="filteredRelationships.length === 0" class="rel-status">无匹配关系</div>
          <div v-else class="rel-list">
            <div v-for="rel in filteredRelationships" :key="rel.id" class="rel-item">
              <span class="rel-predicate">{{ rel.predicate }}</span>
              <span class="rel-arrow">{{ rel.isOutgoing ? '→' : '←' }}</span>
              <span class="rel-target">{{ rel.targetNodeName }}</span>
              <button @click="handleDeleteRelationship(rel)" class="rel-delete-btn" title="删除关系">×</button>
            </div>
          </div>

          <!-- 添加子节点 -->
          <div class="rel-add-section">
            <div class="rel-add-form">
              <select v-model="newChild.predicate" class="form-select">
                <option value="" disabled>选择关系类型</option>
                <option v-for="p in predicateOptions" :key="p" :value="p">{{ p }}</option>
              </select>
              <input
                v-model="newChild.name"
                type="text"
                class="form-input"
                placeholder="输入新节点名称"
              />
            </div>
            <button @click="handleAddChildNode" class="btn btn-primary rel-add-btn-full" :disabled="!canAddChild || addingChild">
              {{ addingChild ? '添加中...' : '添加节点' }}
            </button>
          </div>
        </div>
      </div>

      <div class="editor-footer">
        <button @click="handleClose" class="btn btn-secondary">
          取消
        </button>
        <button @click="handleSave" class="btn btn-primary">
          保存
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { requestJson } from '@/utils/request'

const props = defineProps({
  visible: { type: Boolean, default: false },
  node: { type: Object, default: null }
})

const emit = defineEmits(['close', 'save', 'relationships-changed'])

const formData = ref({ name: '', category: '景点', description: '' })
const relationships = ref([])
const relLoading = ref(false)
const relChanged = ref(false)

// 筛选状态
const filterPredicate = ref('')
const filterKeyword = ref('')
const filterKeywordInput = ref('')

// 添加子节点状态
const newChild = ref({ predicate: '', name: '' })
const addingChild = ref(false)

const canAddChild = computed(() =>
  newChild.value.predicate.trim() && newChild.value.name.trim()
)

// 类型筛选优先于名称搜索
const filteredRelationships = computed(() => {
  let list = relationships.value
  if (filterPredicate.value) {
    list = list.filter(r => r.predicate === filterPredicate.value)
  }
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    list = list.filter(r => r.targetNodeName?.toLowerCase().includes(kw))
  }
  return list
})

const defaultPredicates = ['相关景点', '相关美食', '相关名人', '相关特产', '相关文化', '包含景点', '相关饮品', '所属城市']

const predicateOptions = computed(() => {
  const fromRels = relationships.value.map(r => r.predicate).filter(Boolean)
  return [...new Set([...defaultPredicates, ...fromRels])].sort()
})

watch(() => props.node, (newNode) => {
  if (newNode) {
    formData.value = {
      name: newNode.name || '',
      category: newNode.category || '景点',
      description: newNode.description || ''
    }
    filterPredicate.value = ''
    filterKeyword.value = ''
    filterKeywordInput.value = ''
    loadRelationships(newNode.id)
  }
}, { immediate: true })

async function loadRelationships(nodeId) {
  relLoading.value = true
  try {
    const rels = await requestJson(`/api/kg/relationships?nodeId=${nodeId}&limit=100`)
    const list = Array.isArray(rels) ? rels : []

    // 获取所有关联节点名称
    const targetIds = list.map(r => r.fromNodeId === nodeId ? r.toNodeId : r.fromNodeId)
    const uniqueIds = [...new Set(targetIds)]
    const nodeResults = await Promise.allSettled(
      uniqueIds.map(id => requestJson(`/api/kg/nodes/${id}`))
    )
    const nodeMap = new Map()
    nodeResults.forEach(r => {
      if (r.status === 'fulfilled' && r.value) nodeMap.set(r.value.id, r.value)
    })

    relationships.value = list.map(r => {
      const isOutgoing = r.fromNodeId === nodeId
      const targetId = isOutgoing ? r.toNodeId : r.fromNodeId
      const targetNode = nodeMap.get(targetId)
      return { ...r, isOutgoing, targetNodeId: targetId, targetNodeName: targetNode?.name || '未知节点' }
    })
  } catch (err) {
    console.error('加载关系失败:', err)
    relationships.value = []
  } finally {
    relLoading.value = false
  }
}

async function handleDeleteRelationship(rel) {
  if (!confirm(`确定删除关系「${rel.predicate} ${rel.isOutgoing ? '→' : '←'} ${rel.targetNodeName}」？`)) return
  try {
    await requestJson(`/api/kg/relationships/${rel.id}`, { method: 'DELETE' })
    relationships.value = relationships.value.filter(r => r.id !== rel.id)
    relChanged.value = true
  } catch (err) {
    alert('删除失败：' + (err.message || '未知错误'))
  }
}

async function handleAddChildNode() {
  if (!canAddChild.value) return
  addingChild.value = true
  try {
    // 创建新节点
    const newNode = await requestJson('/api/kg/nodes', {
      method: 'POST',
      body: JSON.stringify({ name: newChild.value.name.trim() })
    })
    // 创建关系：当前节点 → 新节点
    const rel = await requestJson('/api/kg/relationships', {
      method: 'POST',
      body: JSON.stringify({
        fromNodeId: props.node.id,
        toNodeId: newNode.id,
        predicate: newChild.value.predicate.trim()
      })
    })
    relationships.value.push({
      ...rel,
      isOutgoing: true,
      targetNodeId: newNode.id,
      targetNodeName: newNode.name
    })
    relChanged.value = true
    newChild.value = { predicate: '', name: '' }
  } catch (err) {
    alert('添加失败：' + (err.message || '未知错误'))
  } finally {
    addingChild.value = false
  }
}

function handleClose() {
  if (relChanged.value) {
    emit('relationships-changed', props.node?.id)
    relChanged.value = false
  }
  emit('close')
}

function handleSave() {
  if (relChanged.value) {
    emit('relationships-changed', props.node?.id)
    relChanged.value = false
  }
  emit('save', { ...props.node, ...formData.value })
}
</script>

<style scoped>
.node-editor-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.node-editor {
  width: 560px;
  max-width: 90vw;
  background: white;
  border-radius: 8px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.editor-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.close-btn {
  width: 32px; height: 32px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 6px;
  background: transparent;
  font-size: 28px; color: #6b7280;
  cursor: pointer; transition: all 0.2s;
}

.close-btn:hover { background: #f3f4f6; color: #1f2937; }

.editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.form-group { margin-bottom: 20px; }

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-textarea { resize: vertical; font-family: inherit; }

/* 筛选控件 */
.rel-filter-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}

.rel-filter-select { flex: 0 0 140px; }
.rel-filter-input { flex: 1; min-width: 0; }

.rel-filter-clear {
  width: 24px; height: 24px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 4px;
  background: transparent;
  font-size: 16px; color: #9ca3af;
  cursor: pointer; flex-shrink: 0;
}

.rel-filter-clear:hover { background: #fee2e2; color: #ef4444; }

/* 关系列表 */
.rel-status {
  padding: 12px;
  font-size: 13px;
  color: #9ca3af;
  text-align: center;
}

.rel-list {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.rel-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 13px;
}

.rel-predicate { color: #6b7280; font-weight: 500; }
.rel-arrow { color: #9ca3af; }
.rel-target { color: #1f2937; font-weight: 500; flex: 1; }

.rel-delete-btn {
  width: 22px; height: 22px;
  display: flex; align-items: center; justify-content: center;
  border: none; border-radius: 4px;
  background: transparent;
  font-size: 14px; color: #9ca3af;
  cursor: pointer; transition: all 0.15s;
  flex-shrink: 0;
}

.rel-delete-btn:hover { background: #fee2e2; color: #ef4444; }

/* 添加关系 */
.rel-add-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.rel-add-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rel-add-btn-full {
  margin-top: 8px;
  width: 100%;
  padding: 10px 16px;
}

.rel-add-btn-full:disabled { opacity: 0.5; cursor: not-allowed; }

/* 底部按钮 */
.editor-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
}

.btn {
  flex: 1;
  padding: 10px 16px;
  border: none; border-radius: 6px;
  font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.2s;
}

.btn-secondary { background: #f3f4f6; color: #374151; }
.btn-secondary:hover { background: #e5e7eb; }
.btn-primary { background: #3b82f6; color: white; }
.btn-primary:hover { background: #2563eb; }
</style>
