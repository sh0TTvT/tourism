import { ref, computed, watch } from 'vue'
import { forceSimulation, forceLink, forceManyBody, forceCenter, forceCollide } from 'd3-force'

export function useGraphLayout(nodes, links) {
  // 布局状态
  const simulation = ref(null)
  const layoutType = ref('force') // 'force' | 'hierarchical' | 'circular'
  const isSimulationRunning = ref(false)

  // 力导向图配置
  const forceConfig = ref({
    linkDistance: 100,
    linkStrength: 0.5,
    chargeStrength: -300,
    collideRadius: 40,
    centerStrength: 0.1
  })

  // 初始化力导向图模拟
  function initForceSimulation(width, height) {
    if (simulation.value) {
      simulation.value.stop()
    }

    simulation.value = forceSimulation()
      .force('link', forceLink()
        .id(d => d.id)
        .distance(forceConfig.value.linkDistance)
        .strength(forceConfig.value.linkStrength)
      )
      .force('charge', forceManyBody()
        .strength(forceConfig.value.chargeStrength)
      )
      .force('center', forceCenter(width / 2, height / 2)
        .strength(forceConfig.value.centerStrength)
      )
      .force('collide', forceCollide()
        .radius(forceConfig.value.collideRadius)
      )
      .alphaDecay(0.02)
      .velocityDecay(0.3)

    return simulation.value
  }

  // 更新模拟数据
  function updateSimulation(newNodes, newLinks) {
    if (!simulation.value) return
    if (!newNodes || !Array.isArray(newNodes)) return
    if (!newLinks || !Array.isArray(newLinks)) return

    // 创建 links 的深拷贝，避免 D3 修改原始数据
    const linksCopy = newLinks.map(link => ({
      ...link,
      source: link.source.id || link.source,
      target: link.target.id || link.target
    }))

    simulation.value
      .nodes(newNodes)
      .force('link').links(linksCopy)

    simulation.value.alpha(0.3).restart()
    isSimulationRunning.value = true
  }

  // 停止模拟
  function stopSimulation() {
    if (simulation.value) {
      simulation.value.stop()
      isSimulationRunning.value = false
    }
  }

  // 重启模拟
  function restartSimulation(alpha = 0.3) {
    if (simulation.value) {
      simulation.value.alpha(alpha).restart()
      isSimulationRunning.value = true
    }
  }

  // 更新力配置
  function updateForceConfig(config) {
    Object.assign(forceConfig.value, config)

    if (simulation.value) {
      if (config.linkDistance !== undefined || config.linkStrength !== undefined) {
        simulation.value.force('link')
          .distance(forceConfig.value.linkDistance)
          .strength(forceConfig.value.linkStrength)
      }

      if (config.chargeStrength !== undefined) {
        simulation.value.force('charge')
          .strength(forceConfig.value.chargeStrength)
      }

      if (config.collideRadius !== undefined) {
        simulation.value.force('collide')
          .radius(forceConfig.value.collideRadius)
      }

      if (config.centerStrength !== undefined) {
        simulation.value.force('center')
          .strength(forceConfig.value.centerStrength)
      }

      restartSimulation(0.3)
    }
  }

  // 固定节点位置
  function pinNode(node) {
    node.fx = node.x
    node.fy = node.y
  }

  // 取消固定节点
  function unpinNode(node) {
    node.fx = null
    node.fy = null
  }

  // 层次布局（简化版）
  function applyHierarchicalLayout(nodeList, linkList, width, height) {
    // 构建层级关系
    const levels = new Map()
    const visited = new Set()

    // 找到根节点（概览根：type === 'root' 或兼容旧的 'city'）
    const roots = nodeList.filter(n => n.type === 'root' || n.type === 'city')

    // BFS 分层
    const queue = roots.map(r => ({ node: r, level: 0 }))
    roots.forEach(r => {
      levels.set(r.id, 0)
      visited.add(r.id)
    })

    while (queue.length > 0) {
      const { node, level } = queue.shift()

      linkList.forEach(link => {
        const sourceId = link.source.id || link.source
        const targetId = link.target.id || link.target

        // 检查双向边
        if (sourceId === node.id && !visited.has(targetId)) {
          const target = nodeList.find(n => n.id === targetId)
          if (target) {
            levels.set(targetId, level + 1)
            visited.add(targetId)
            queue.push({ node: target, level: level + 1 })
          }
        } else if (targetId === node.id && !visited.has(sourceId)) {
          const source = nodeList.find(n => n.id === sourceId)
          if (source) {
            levels.set(sourceId, level + 1)
            visited.add(sourceId)
            queue.push({ node: source, level: level + 1 })
          }
        }
      })
    }

    // 按层级分组
    const levelGroups = new Map()
    nodeList.forEach(node => {
      const level = levels.get(node.id) || 0
      if (!levelGroups.has(level)) {
        levelGroups.set(level, [])
      }
      levelGroups.get(level).push(node)
    })

    // 计算位置
    const levelHeight = height / (levelGroups.size + 1)

    levelGroups.forEach((nodesInLevel, level) => {
      const levelWidth = width / (nodesInLevel.length + 1)
      nodesInLevel.forEach((node, index) => {
        node.x = levelWidth * (index + 1)
        node.y = levelHeight * (level + 1)
      })
    })
  }

  // 环形布局
  function applyCircularLayout(nodeList, width, height) {
    const centerX = width / 2
    const centerY = height / 2
    const radius = Math.min(width, height) * 0.35

    nodeList.forEach((node, index) => {
      const angle = (2 * Math.PI * index) / nodeList.length
      node.x = centerX + radius * Math.cos(angle)
      node.y = centerY + radius * Math.sin(angle)
    })
  }

  // 切换布局类型
  function setLayoutType(type, nodeList, linkList, width, height) {
    const validTypes = ['force', 'hierarchical', 'circular']
    if (!validTypes.includes(type)) {
      console.warn(`Invalid layout type: ${type}, falling back to 'force'`)
      type = 'force'
    }

    layoutType.value = type

    if (type === 'hierarchical') {
      stopSimulation()
      applyHierarchicalLayout(nodeList, linkList, width, height)
    } else if (type === 'circular') {
      stopSimulation()
      applyCircularLayout(nodeList, width, height)
    } else {
      // force layout
      if (!simulation.value) {
        initForceSimulation(width, height)
      }
      updateSimulation(nodeList, linkList)
    }
  }

  return {
    // 状态
    simulation,
    layoutType,
    isSimulationRunning,
    forceConfig,

    // 方法
    initForceSimulation,
    updateSimulation,
    stopSimulation,
    restartSimulation,
    updateForceConfig,
    pinNode,
    unpinNode,
    setLayoutType
  }
}
