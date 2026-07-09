import { ref, computed } from 'vue'

export function useGraphNavigation() {
  // 导航栈：记录每个层级的状态
  // 每个层级记录：该层级的节点ID列表（用于层级式展示）
  const navigationStack = ref([
    {
      level: 0,
      label: '概览',
      nodeId: null,
      expandedNodeIds: [],
      visibleNodeIds: [] // 该层级可见的节点ID列表
    }
  ])

  // 当前层级
  const currentLevel = computed(() => navigationStack.value.length - 1)

  // 当前层级状态
  const currentState = computed(() =>
    navigationStack.value[navigationStack.value.length - 1]
  )

  // 面包屑路径
  const breadcrumbs = computed(() =>
    navigationStack.value.map((state, index) => ({
      level: index,
      label: state.label,
      nodeId: state.nodeId,
      isActive: index === currentLevel.value
    }))
  )

  // 当前层级可见的节点ID集合
  const currentVisibleNodeIds = computed(() => {
    return new Set(currentState.value?.visibleNodeIds || [])
  })

  // 展开节点：进入下一层级，只显示子节点
  function expandNode(nodeId, nodeName, childNodeIds = [], isSearchResult = false) {
    if (!nodeId) return

    const newState = {
      level: currentLevel.value + 1,
      label: nodeName || `节点 ${nodeId}`,
      nodeId,
      expandedNodeIds: [nodeId],
      visibleNodeIds: childNodeIds, // 新层级只显示子节点
      isSearchResult // 标记是否为搜索结果
    }

    navigationStack.value.push(newState)
  }

  // 导航到搜索结果：清空导航栈，只保留概览和搜索结果
  function navigateToSearchResult(nodeId, nodeName) {
    navigationStack.value = [
      navigationStack.value[0], // 保留概览层级
      {
        level: 1,
        label: `搜索: ${nodeName}`,
        nodeId,
        expandedNodeIds: [],
        visibleNodeIds: [],
        isSearchResult: true
      }
    ]
  }

  // 设置当前层级的可见节点
  function setCurrentVisibleNodes(nodeIds) {
    const current = currentState.value
    if (current) {
      current.visibleNodeIds = nodeIds
    }
  }

  // 在当前层级添加展开的节点
  function addExpandedNode(nodeId) {
    if (!nodeId) return

    const current = currentState.value
    if (current && !current.expandedNodeIds.includes(nodeId)) {
      current.expandedNodeIds.push(nodeId)
    }
  }

  // 收起节点
  function collapseNode(nodeId) {
    if (!nodeId) return

    const current = currentState.value
    if (current) {
      const index = current.expandedNodeIds.indexOf(nodeId)
      if (index > -1) {
        current.expandedNodeIds.splice(index, 1)
      }
    }
  }

  // 跳转到指定层级
  function navigateToLevel(level) {
    if (level >= 0 && level < navigationStack.value.length) {
      navigationStack.value = navigationStack.value.slice(0, level + 1)
    }
  }

  // 返回上一层
  function navigateBack() {
    if (navigationStack.value.length > 1) {
      navigationStack.value.pop()
    }
  }

  // 返回概览
  function navigateToOverview() {
    navigationStack.value = [navigationStack.value[0]]
  }

  // 获取所有展开的节点 ID
  const allExpandedNodeIds = computed(() => {
    const allIds = []
    navigationStack.value.forEach(state => {
      allIds.push(...state.expandedNodeIds)
    })
    return Array.from(new Set(allIds))
  })

  // 检查节点是否已展开
  function isNodeExpanded(nodeId) {
    return allExpandedNodeIds.value.includes(nodeId)
  }

  // 重置导航状态
  function resetNavigation() {
    navigationStack.value = [
      {
        level: 0,
        label: '概览',
        nodeId: null,
        expandedNodeIds: []
      }
    ]
  }

  return {
    // 状态
    navigationStack,
    currentLevel,
    currentState,
    breadcrumbs,
    allExpandedNodeIds,
    currentVisibleNodeIds,

    // 方法
    expandNode,
    addExpandedNode,
    collapseNode,
    navigateToLevel,
    navigateBack,
    navigateToOverview,
    navigateToSearchResult,
    isNodeExpanded,
    resetNavigation,
    setCurrentVisibleNodes
  }
}
