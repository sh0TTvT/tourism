<template>
  <div class="knowledge-graph-view">
    <!-- 工具栏 -->
    <GraphToolbar
      ref="toolbarRef"
      @navigate-to-node="handleNavigateToNode"
      @zoom-in="handleZoomIn"
      @zoom-out="handleZoomOut"
      @zoom-reset="handleZoomReset"
      @layout-change="handleLayoutChange"
      @back-to-overview="handleBackToOverview"
    />

    <!-- 面包屑导航 -->
    <GraphBreadcrumb
      :breadcrumbs="breadcrumbs"
      @navigate="handleBreadcrumbNavigate"
    />

    <!-- 图谱画布 -->
    <GraphCanvas
      :nodes="visibleNodes"
      :links="visibleLinks"
      @node-click="handleNodeClick"
      @node-double-click="handleNodeDoubleClick"
      @node-hover="handleNodeHover"
      @node-unhover="handleNodeUnhover"
    />

    <!-- 节点详情面板 -->
    <NodeDetailPanel
      :visible="showDetailPanel"
      :node="detailNode"
      :relationships="nodeRelationships"
      @close="showDetailPanel = false"
      @edit-node="handleEditNode"
      @expand-node="handleExpandNode"
      @navigate-to-node="handleNavigateToNode"
    />

    <!-- 节点编辑器 -->
    <NodeEditor
      :visible="showEditor"
      :node="editingNode"
      @close="showEditor = false"
      @save="handleSaveNode"
      @relationships-changed="handleRelationshipsChanged"
    />

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-overlay">
      <div class="loading-spinner"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, provide, onMounted, watch } from 'vue'
import GraphToolbar from './components/GraphToolbar.vue'
import GraphBreadcrumb from './components/GraphBreadcrumb.vue'
import GraphCanvas from './components/GraphCanvas.vue'
import NodeDetailPanel from './components/NodeDetailPanel.vue'
import NodeEditor from './components/NodeEditor.vue'
import { useGraphData } from './composables/useGraphData'
import { useGraphLayout } from './composables/useGraphLayout'
import { useGraphInteraction } from './composables/useGraphInteraction'
import { useGraphNavigation } from './composables/useGraphNavigation'
import { requestJson } from '@/utils/request'

// 概览根层级类别：国家→省份→城市→区县→景点/酒店/美食/名人
const ROOT_CATEGORY = '国家'

// 层级映射：每个 category 展开时仅显示其"下一层"对应的子类别
// 数据结构：国家→省份→城市→区县→叶子节点（景点/酒店/餐厅/特产等）
// 名人节点保持在城市级别（因为名人与整个城市相关）
const LEAF_CATEGORIES = ['景点', '酒店', '餐厅', '名人', '特产', '文化',
  '美食', '主菜', '主食', '小吃', '点心', '汤羹', '饮品', '早餐', '面食']
const LEVEL_HIERARCHY = {
  国家: ['省份'],
  省份: ['城市'],
  城市: ['区县'],
  区县: LEAF_CATEGORIES,
  // 景点可嵌套：如 西湖 → 苏堤春晓（西湖十景）
  景点: ['景点']
}

// Composables
const {
  nodes,
  links,
  loading,
  error,
  loadRootNodes,
  expandNode: expandNodeApi,
  loadNodeRelationships,
  getNode,
  getNodes
} = useGraphData()

const {
  simulation,
  layoutType,
  initForceSimulation,
  updateSimulation,
  setLayoutType
} = useGraphLayout(nodes, links)

const {
  selectedNodeId,
  hoveredNodeId,
  transform,
  initZoom,
  zoomTo,
  zoomToNode,
  resetZoom,
  createNodeDrag,
  selectNode,
  deselectNode,
  hoverNode,
  unhoverNode
} = useGraphInteraction()

const {
  breadcrumbs,
  currentLevel,
  expandNode,
  addExpandedNode,
  collapseNode,
  navigateToLevel,
  navigateBack,
  navigateToOverview,
  navigateToSearchResult,
  isNodeExpanded,
  allExpandedNodeIds,
  currentVisibleNodeIds,
  setCurrentVisibleNodes
} = useGraphNavigation()

// 提供全局状态
provide('graphState', {
  simulation,
  transform,
  selectedNodeId,
  hoveredNodeId,
  initZoom,
  createNodeDrag
})

// UI 状态
const toolbarRef = ref(null)
const showDetailPanel = ref(false)
const showEditor = ref(false)
const editingNode = ref(null)
const detailNode = ref(null)
const nodeRelationships = ref([])
const clickTimer = ref(null)

// 可见节点和边：层级式展示，只显示当前层级的节点
const visibleNodes = computed(() => {
  const current = currentVisibleNodeIds.value
  // 概览层级（level 0）没有设置 visibleNodeIds，显示所有根节点
  if (currentLevel.value === 0) {
    return nodes.value.filter(n => n.category === ROOT_CATEGORY)
  }
  // 其他层级：只显示 visibleNodeIds 中的节点
  // 即使是空数组也不显示任何节点（叶子节点的情况）
  return nodes.value.filter(n => current.has(n.id))
})

const visibleLinks = computed(() => {
  const visibleNodeIds = new Set(visibleNodes.value.map(n => n.id))
  return links.value.filter(link => {
    const sourceId = link.source.id || link.source
    const targetId = link.target.id || link.target
    return visibleNodeIds.has(sourceId) && visibleNodeIds.has(targetId)
  })
})

// 选中的节点对象
const selectedNode = computed(() => {
  if (!selectedNodeId.value) return null
  return nodes.value.find(n => n.id === selectedNodeId.value)
})

// 初始化
onMounted(async () => {
  try {
    await loadRootNodes(ROOT_CATEGORY)

    // 设置概览层级的可见节点
    const rootNodeIds = nodes.value.map(n => n.id)
    setCurrentVisibleNodes(rootNodeIds)

    // 初始化力导向图
    const container = document.querySelector('.graph-canvas')
    if (container) {
      const width = container.clientWidth
      const height = container.clientHeight
      initForceSimulation(width, height)
      updateSimulation(nodes.value, links.value)
    }
  } catch (err) {
    console.error('初始化知识图谱失败:', err)
  }
})

// 监听节点和边变化，更新模拟
watch([nodes, links], () => {
  if (simulation.value) {
    updateSimulation(nodes.value, links.value)
  }
})

// 事件处理
async function handleNodeClick(node) {
  // 清除之前的单击定时器
  if (clickTimer.value) {
    clearTimeout(clickTimer.value)
    clickTimer.value = null
  }

  // 延迟 300ms 执行单击逻辑，避免与双击冲突
  clickTimer.value = setTimeout(async () => {
    selectNode(node.id)

    // 检查节点是否有子类别（是否为叶子节点）
    const allowedNextCats = LEVEL_HIERARCHY[node.category] || []
    const isLeafNode = allowedNextCats.length === 0

    // 叶子节点：直接显示详情面板，不展开
    if (isLeafNode) {
      await showNodeDetail(node)
      return
    }

    // 非叶子节点：如果未展开则展开
    if (!isNodeExpanded(node.id)) {
      await handleExpandNode(node)
    }

    clickTimer.value = null
  }, 300)
}

async function loadAndEnrichRelationships(nodeId) {
  const relationships = await loadNodeRelationships(nodeId)

  const targetIds = relationships.map(rel =>
    rel.fromNodeId === nodeId ? rel.toNodeId : rel.fromNodeId
  )
  const uniqueIds = [...new Set(targetIds)]
  const targetNodes = await getNodes(uniqueIds)
  const nodeMap = new Map(targetNodes.map(n => [n.id, n]))

  return relationships.map(rel => {
    const isOutgoing = rel.fromNodeId === nodeId
    const targetId = isOutgoing ? rel.toNodeId : rel.fromNodeId
    const targetNode = nodeMap.get(targetId)
    return {
      ...rel,
      targetNodeId: targetId,
      targetNodeName: targetNode?.name || '未知节点',
      isOutgoing
    }
  })
}

async function showNodeDetail(node) {
  detailNode.value = node
  showDetailPanel.value = true
  try {
    nodeRelationships.value = await loadAndEnrichRelationships(node.id)
  } catch (err) {
    console.error('加载节点关系失败:', err)
    nodeRelationships.value = []
  }
}

async function handleNodeDoubleClick(node) {
  // 清除单击定时器，避免双击时触发单击逻辑
  if (clickTimer.value) {
    clearTimeout(clickTimer.value)
    clickTimer.value = null
  }

  selectNode(node.id)
  await showNodeDetail(node)
}

function handleNodeHover(node) {
  hoverNode(node.id)
}

function handleNodeUnhover() {
  unhoverNode()
}

async function handleExpandNode(node) {
  if (isNodeExpanded(node.id)) return
  try {
    const { nodes: childNodes, links: childLinks } = await expandNodeApi(node.id)

    // 按层级映射过滤：仅保留当前 category 对应"下一层"的子节点
    const allowedNextCats = LEVEL_HIERARCHY[node.category] || []
    const filteredChildNodes = allowedNextCats.length === 0
      ? []
      : childNodes.filter(n => allowedNextCats.includes(n.category))

    // 如果没有子节点，显示详情面板而不是进入空层级
    if (filteredChildNodes.length === 0) {
      await showNodeDetail(node)
      return
    }

    const allowedChildIds = new Set(filteredChildNodes.map(n => n.id))
    const filteredChildLinks = childLinks.filter(l => {
      const sId = l.source?.id || l.source
      const tId = l.target?.id || l.target
      // 仅保留 self↔allowedChild 的边
      return (sId === node.id && allowedChildIds.has(tId))
        || (tId === node.id && allowedChildIds.has(sId))
    })

    // 去重：已在画布上的节点/边不重复加入
    const existingIds = new Set(nodes.value.map(n => n.id))
    const newNodes = filteredChildNodes.filter(n => !existingIds.has(n.id))
    const existingLinkIds = new Set(links.value.map(l => l.id))
    const newLinks = filteredChildLinks.filter(l => !existingLinkIds.has(l.id))

    // 创建新的数组副本，避免 D3 修改原始响应式数据
    nodes.value = [...nodes.value, ...newNodes]
    links.value = [...links.value, ...newLinks]

    // 标记节点为已展开
    const parentNode = nodes.value.find(n => n.id === node.id)
    if (parentNode) {
      parentNode.expanded = true
    }

    // 更新导航状态：进入下一层级，只显示子节点
    const childNodeIds = filteredChildNodes.map(n => n.id)
    expandNode(node.id, node.name, childNodeIds)

    // 缩放到节点
    const container = document.querySelector('.graph-canvas')
    if (container) {
      zoomToNode(node, container.clientWidth, container.clientHeight, 1.5)
    }
  } catch (err) {
    console.error('展开节点失败:', err)
  }
}

function handleEditNode(node) {
  editingNode.value = node
  showEditor.value = true
  showDetailPanel.value = false
}

async function handleRelationshipsChanged(nodeId) {
  if (nodeId && detailNode.value?.id === nodeId) {
    try {
      nodeRelationships.value = await loadAndEnrichRelationships(nodeId)
    } catch (err) {
      console.error('刷新关系失败:', err)
    }
  }
}

async function handleSaveNode(updatedNode) {
  try {
    await requestJson(`/api/kg/nodes/${updatedNode.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: updatedNode.name,
        category: updatedNode.category,
        description: updatedNode.description
      })
    })

    // 更新本地节点数据
    const node = nodes.value.find(n => n.id === updatedNode.id)
    if (node) {
      Object.assign(node, updatedNode)
    }

    showEditor.value = false
  } catch (err) {
    console.error('保存节点失败:', err)
    alert('保存失败：' + err.message)
  }
}

function handleZoomIn() {
  const newScale = Math.min(transform.value.k * 1.3, 4)
  zoomTo(newScale)
}

function handleZoomOut() {
  const newScale = Math.max(transform.value.k / 1.3, 0.1)
  zoomTo(newScale)
}

function handleZoomReset() {
  const container = document.querySelector('.graph-canvas')
  if (container) {
    resetZoom(container.clientWidth, container.clientHeight)
  }
}

function handleLayoutChange(layout) {
  const container = document.querySelector('.graph-canvas')
  if (container) {
    setLayoutType(
      layout,
      nodes.value,
      links.value,
      container.clientWidth,
      container.clientHeight
    )
  }
}

async function handleBackToOverview() {
  navigateToOverview()
  deselectNode()
  showDetailPanel.value = false
  toolbarRef.value?.clearSearch()

  // 重新加载概览根层级（国家）节点
  try {
    await loadRootNodes(ROOT_CATEGORY)

    // 设置概览层级的可见节点
    const rootNodeIds = nodes.value.map(n => n.id)
    setCurrentVisibleNodes(rootNodeIds)

    // 重新初始化力导向图
    const container = document.querySelector('.graph-canvas')
    if (container) {
      const width = container.clientWidth
      const height = container.clientHeight
      updateSimulation(nodes.value, links.value)
    }
  } catch (err) {
    console.error('返回概览失败:', err)
  }

  handleZoomReset()
}

function handleBreadcrumbNavigate(level) {
  navigateToLevel(level)
  deselectNode()
  showDetailPanel.value = false

  // 根据目标层级恢复可见节点
  if (level === 0) {
    handleBackToOverview()
  } else {
    // 其他层级：显示该层级记录的可见节点
    // 重置可见节点的位置，避免挤在一起
    const container = document.querySelector('.graph-canvas')
    if (container) {
      const width = container.clientWidth
      const height = container.clientHeight
      const centerX = width / 2
      const centerY = height / 2

      // 给可见节点分配随机初始位置
      visibleNodes.value.forEach(node => {
        // 在中心周围随机分布
        const angle = Math.random() * 2 * Math.PI
        const radius = Math.random() * 200 + 100
        node.x = centerX + Math.cos(angle) * radius
        node.y = centerY + Math.sin(angle) * radius
        // 清除固定位置
        node.fx = null
        node.fy = null
      })

      // 重新启动力导向模拟
      if (simulation.value) {
        updateSimulation(nodes.value, links.value)
      }
    }

    handleZoomReset()
  }
}

async function handleNavigateToNode(nodeId) {
  const node = await getNode(nodeId)
  if (!node) return

  // 使用简化的搜索面包屑：概览 / 搜索: 节点名称
  navigateToSearchResult(nodeId, node.name)

  // 清空当前图谱，只显示搜索到的节点
  nodes.value = [node]
  links.value = []

  // 设置可见节点
  setCurrentVisibleNodes([nodeId])

  selectNode(nodeId)

  // 等待 D3 力导向布局分配坐标
  await new Promise(r => setTimeout(r, 500))
  const target = nodes.value.find(n => n.id === nodeId)
  const container = document.querySelector('.graph-canvas')
  if (container && target) {
    zoomToNode(target, container.clientWidth, container.clientHeight, 2)
  }

  await showNodeDetail(node)
}
</script>

<style scoped>
.knowledge-graph-view {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}

.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
