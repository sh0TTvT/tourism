import { ref, computed } from 'vue'
import { requestJson } from '@/utils/request'

export function useGraphData() {
  // 数据状态
  const nodes = ref([])
  const links = ref([])
  const nodeCache = ref(new Map())
  const loading = ref(false)
  const error = ref(null)

  // 缓存节点数据
  function cacheNodes(nodeList) {
    if (!Array.isArray(nodeList) || !nodeList.length) return

    nodeList.forEach(node => {
      if (node?.id) {
        nodeCache.value.set(node.id, node)
      }
    })
  }

  // 清除缓存：支持清除单个节点或全部缓存
  function invalidateCache(nodeId) {
    if (nodeId) {
      nodeCache.value.delete(nodeId)
    } else {
      nodeCache.value.clear()
    }
  }

  // 从缓存或 API 获取节点
  async function getNode(nodeId) {
    if (nodeCache.value.has(nodeId)) {
      return nodeCache.value.get(nodeId)
    }

    try {
      const node = await requestJson(`/api/kg/nodes/${nodeId}`)
      cacheNodes([node])
      return node
    } catch (err) {
      console.error(`获取节点 ${nodeId} 失败:`, err)
      throw err
    }
  }

  // 批量获取节点
  async function getNodes(nodeIds) {
    const missingIds = nodeIds.filter(id => !nodeCache.value.has(id))

    if (missingIds.length === 0) {
      return nodeIds.map(id => nodeCache.value.get(id)).filter(Boolean)
    }

    try {
      const results = await Promise.allSettled(
        missingIds.map(id => requestJson(`/api/kg/nodes/${id}`))
      )

      const fetchedNodes = results
        .filter(r => r.status === 'fulfilled')
        .map(r => r.value)

      cacheNodes(fetchedNodes)

      return nodeIds.map(id => nodeCache.value.get(id)).filter(Boolean)
    } catch (err) {
      console.error('批量获取节点失败:', err)
      throw err
    }
  }

  // 加载某一类别的所有节点作为概览根（默认国家）
  async function loadRootNodes(category = '国家') {
    loading.value = true
    error.value = null

    try {
      const encoded = encodeURIComponent(category)
      const data = await requestJson(`/api/kg/nodes/by-category/${encoded}`)
      const rootNodes = Array.isArray(data) ? data : []

      nodes.value = rootNodes.map(node => ({
        id: node.id,
        name: node.name,
        category: node.category,
        description: node.description,
        childCount: node.childCount || 0,
        type: 'root',
        expanded: false
      }))

      cacheNodes(rootNodes)

      // 加载同层级节点之间的关系（如多个国家之间）
      try {
        const sameLevelRels = await requestJson(`/api/kg/relationships/by-category/${encoded}`)
        links.value = Array.isArray(sameLevelRels)
          ? sameLevelRels.map(rel => ({
              id: rel.id,
              source: rel.fromNodeId,
              target: rel.toNodeId,
              predicate: rel.predicate,
              weight: rel.weight || 1
            }))
          : []
      } catch (err) {
        console.error('加载同层级关系失败:', err)
        links.value = []
        if (!error.value) {
          error.value = '部分数据加载失败'
        }
      }

      return { nodes: nodes.value, links: links.value }
    } catch (err) {
      error.value = err.message || '加载根节点失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 兼容旧调用：默认入口已改为国家层
  async function loadCityNodes() {
    return loadRootNodes('城市')
  }

  // 展开任意节点：通用接口，后端按谓词识别下级方向
  // 数据库存在双向冗余边（如"包含区县" + "所属城市"），需对子节点按 id 去重
  async function expandNode(nodeId) {
    loading.value = true
    error.value = null

    try {
      const data = await requestJson(`/api/kg/nodes/${nodeId}/expand`)
      const rawChildNodes = data.nodes || []
      const relationships = data.edges || data.relationships || []

      cacheNodes(rawChildNodes)

      // 同一 child 可能通过两条不同 predicate 边返回，按 id 去重
      const seenChildIds = new Set()
      const uniqueChildNodes = rawChildNodes.filter(n => {
        if (n?.id == null || seenChildIds.has(n.id)) return false
        seenChildIds.add(n.id)
        return true
      })

      return {
        nodes: uniqueChildNodes.map(node => ({
          id: node.id,
          name: node.name,
          category: node.category,
          description: node.description,
          type: 'child',
          parentNodeId: nodeId
        })),
        links: relationships.map(rel => ({
          id: rel.id,
          source: rel.fromNodeId,
          target: rel.toNodeId,
          predicate: rel.predicate,
          weight: rel.weight || 1
        }))
      }
    } catch (err) {
      error.value = err.message || '展开节点失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  // 兼容旧调用名
  async function expandCityNode(cityId) {
    return expandNode(cityId)
  }

  // 加载节点关系
  async function loadNodeRelationships(nodeId, limit = 100) {
    try {
      const relationships = await requestJson(`/api/kg/relationships?nodeId=${nodeId}&limit=${limit}`)
      return Array.isArray(relationships) ? relationships : []
    } catch (err) {
      console.error(`加载节点 ${nodeId} 关系失败:`, err)
      return []
    }
  }

  // 搜索全库节点
  async function searchNodes(keyword, limit = 20) {
    if (!keyword || !keyword.trim()) return []
    try {
      const results = await requestJson(`/api/kg/nodes?keyword=${encodeURIComponent(keyword.trim())}&limit=${limit}`)
      const list = Array.isArray(results) ? results : []
      cacheNodes(list)
      return list
    } catch (err) {
      console.error('搜索节点失败:', err)
      return []
    }
  }

  return {
    // 状态
    nodes,
    links,
    nodeCache,
    loading,
    error,

    // 方法
    cacheNodes,
    invalidateCache,
    getNode,
    getNodes,
    loadRootNodes,
    loadCityNodes,
    expandNode,
    expandCityNode,
    loadNodeRelationships,
    searchNodes
  }
}
