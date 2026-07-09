<template>
  <div ref="containerRef" class="graph-canvas">
    <svg ref="svgRef" class="graph-svg"></svg>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, inject } from 'vue'
import { select } from 'd3-selection'
import {
  initSvgStructure,
  renderNodes,
  renderLinks,
  updatePositions
} from '../utils/graphRenderer'

const props = defineProps({
  nodes: {
    type: Array,
    required: true
  },
  links: {
    type: Array,
    required: true
  }
})

const emit = defineEmits([
  'node-click',
  'node-double-click',
  'node-hover',
  'node-unhover'
])

// 注入全局状态
const graphState = inject('graphState')
const {
  simulation,
  transform,
  selectedNodeId,
  hoveredNodeId,
  initZoom,
  createNodeDrag
} = graphState

// 引用
const containerRef = ref(null)
const svgRef = ref(null)
let svgSelection = null
let containerGroup = null
let nodeSelection = null
let linkSelection = null
let resizeObserver = null

// 初始化
onMounted(() => {
  initCanvas()
  setupResizeObserver()
})

onUnmounted(() => {
  cleanup()
})

// 监听数据变化
watch(() => [props.nodes, props.links], () => {
  renderGraph()
}, { deep: true })

// 监听选中状态变化
watch([selectedNodeId, hoveredNodeId], () => {
  updateNodeStyles()
})

// 监听变换变化
watch(transform, (newTransform) => {
  if (containerGroup) {
    containerGroup.attr('transform',
      `translate(${newTransform.x},${newTransform.y}) scale(${newTransform.k})`
    )
  }
})

function initCanvas() {
  if (!svgRef.value || !containerRef.value) return

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  svgSelection = select(svgRef.value)
  svgSelection
    .attr('width', width)
    .attr('height', height)

  // 初始化 SVG 结构
  containerGroup = initSvgStructure(svgRef.value)

  // 初始化缩放
  initZoom(svgRef.value, (newTransform) => {
    containerGroup.attr('transform',
      `translate(${newTransform.x},${newTransform.y}) scale(${newTransform.k})`
    )
  })

  // 渲染图谱
  renderGraph()
}

function renderGraph() {
  if (!containerGroup || !props.nodes || !props.links) return

  // 创建拖拽行为
  const dragBehavior = createNodeDrag(simulation.value)

  // 渲染边
  linkSelection = renderLinks(containerGroup, props.links, props.nodes)

  // 渲染节点
  nodeSelection = renderNodes(containerGroup, props.nodes, {
    onNodeClick: handleNodeClick,
    onNodeDoubleClick: handleNodeDoubleClick,
    onNodeMouseEnter: handleNodeMouseEnter,
    onNodeMouseLeave: handleNodeMouseLeave,
    selectedNodeId: selectedNodeId.value,
    hoveredNodeId: hoveredNodeId.value,
    dragBehavior
  })

  // 监听模拟更新
  if (simulation.value) {
    simulation.value.on('tick', () => {
      updatePositions(nodeSelection, linkSelection)
    })
  }
}

function updateNodeStyles() {
  if (!nodeSelection) return

  nodeSelection.each(function(d) {
    const node = select(this)
    const isSelected = d.id === selectedNodeId.value
    const isHovered = d.id === hoveredNodeId.value

    node.select('.node-circle')
      .attr('stroke-width', isSelected ? 3 : 2)
      .attr('opacity', isSelected || isHovered ? 1 : 0.9)
  })
}

function handleNodeClick(node) {
  emit('node-click', node)
}

function handleNodeDoubleClick(node) {
  emit('node-double-click', node)
}

function handleNodeMouseEnter(node) {
  emit('node-hover', node)
}

function handleNodeMouseLeave(node) {
  emit('node-unhover', node)
}

function setupResizeObserver() {
  if (!containerRef.value) return

  resizeObserver = new ResizeObserver(() => {
    if (svgRef.value && containerRef.value) {
      const width = containerRef.value.clientWidth
      const height = containerRef.value.clientHeight

      select(svgRef.value)
        .attr('width', width)
        .attr('height', height)
    }
  })

  resizeObserver.observe(containerRef.value)
}

function cleanup() {
  if (simulation.value) {
    simulation.value.stop()
  }

  if (resizeObserver) {
    resizeObserver.disconnect()
  }
}
</script>

<style scoped>
.graph-canvas {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  background: #fafafa;
}

.graph-svg {
  display: block;
  cursor: grab;
}

.graph-svg:active {
  cursor: grabbing;
}
</style>
