import { ref, computed } from 'vue'
import { zoom as d3Zoom, zoomIdentity } from 'd3-zoom'
import { select } from 'd3-selection'
import { drag as d3Drag } from 'd3-drag'

export function useGraphInteraction() {
  // 交互状态
  const selectedNodeId = ref(null)
  const hoveredNodeId = ref(null)
  const transform = ref({ x: 0, y: 0, k: 1 })
  const isDragging = ref(false)

  // 缩放控制器
  let zoomBehavior = null
  let svgSelection = null

  // 初始化缩放行为
  function initZoom(svgElement, onZoom) {
    svgSelection = select(svgElement)

    zoomBehavior = d3Zoom()
      .scaleExtent([0.1, 4])
      .on('zoom', (event) => {
        transform.value = event.transform
        if (onZoom) {
          onZoom(event.transform)
        }
      })

    svgSelection.call(zoomBehavior)

    return zoomBehavior
  }

  // 缩放到指定级别
  function zoomTo(scale, duration = 300) {
    if (!svgSelection || !zoomBehavior) return

    const currentTransform = transform.value
    const newTransform = zoomIdentity
      .translate(currentTransform.x, currentTransform.y)
      .scale(scale)

    svgSelection
      .transition()
      .duration(duration)
      .call(zoomBehavior.transform, newTransform)
  }

  // 缩放到节点
  function zoomToNode(node, width, height, scale = 1.5, duration = 750) {
    if (!node || typeof node.x !== 'number' || typeof node.y !== 'number') {
      console.warn('zoomToNode: invalid node parameter')
      return
    }
    if (!svgSelection || !zoomBehavior) return

    const x = width / 2 - node.x * scale
    const y = height / 2 - node.y * scale

    const newTransform = zoomIdentity
      .translate(x, y)
      .scale(scale)

    svgSelection
      .transition()
      .duration(duration)
      .call(zoomBehavior.transform, newTransform)
  }

  // 重置缩放
  function resetZoom(duration = 300) {
    if (!svgSelection || !zoomBehavior) return

    const newTransform = zoomIdentity
      .translate(0, 0)
      .scale(1)

    svgSelection
      .transition()
      .duration(duration)
      .call(zoomBehavior.transform, newTransform)
  }

  // 创建节点拖拽行为
  function createNodeDrag(simulation) {
    return d3Drag()
      .on('start', (event, d) => {
        isDragging.value = true
        if (!event.active && simulation) {
          simulation.alphaTarget(0.3).restart()
        }
        d.fx = d.x
        d.fy = d.y
      })
      .on('drag', (event, d) => {
        d.fx = event.x
        d.fy = event.y
      })
      .on('end', (event, d) => {
        isDragging.value = false
        if (!event.active && simulation) {
          simulation.alphaTarget(0)
        }
        // 拖拽结束后保持节点固定位置，用户可通过双击或其他操作取消固定
      })
  }

  // 选择节点
  function selectNode(nodeId) {
    selectedNodeId.value = nodeId
  }

  // 取消选择
  function deselectNode() {
    selectedNodeId.value = null
  }

  // 悬停节点
  function hoverNode(nodeId) {
    hoveredNodeId.value = nodeId
  }

  // 取消悬停
  function unhoverNode() {
    hoveredNodeId.value = null
  }

  // 检查节点是否被选中
  const isNodeSelected = computed(() => (nodeId) => {
    return selectedNodeId.value === nodeId
  })

  // 检查节点是否被悬停
  const isNodeHovered = computed(() => (nodeId) => {
    return hoveredNodeId.value === nodeId
  })

  // 清理资源
  function cleanup() {
    if (svgSelection) {
      svgSelection.on('.zoom', null)
    }
    zoomBehavior = null
    svgSelection = null
  }

  return {
    // 状态
    selectedNodeId,
    hoveredNodeId,
    transform,
    isDragging,

    // 计算属性
    isNodeSelected,
    isNodeHovered,

    // 方法
    initZoom,
    zoomTo,
    zoomToNode,
    resetZoom,
    createNodeDrag,
    selectNode,
    deselectNode,
    hoverNode,
    unhoverNode,
    cleanup
  }
}
