<template>
  <div class="kg-canvas-container">
    <svg
      ref="svgRef"
      class="kg-canvas"
      @click="handleCanvasClick"
    ></svg>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue';
import * as d3 from 'd3';

const props = defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  edges: {
    type: Array,
    default: () => []
  },
  selectedNodeId: {
    type: Number,
    default: null
  },
  viewMode: {
    type: String,
    default: 'overview' // 'overview' | 'expanded' | 'focused'
  }
});

const emit = defineEmits(['node-click', 'node-hover', 'canvas-click']);

const svgRef = ref(null);
let svg = null;
let g = null;
let simulation = null;
let zoom = null;

// 节点和边的数据
let nodeElements = null;
let linkElements = null;
let labelElements = null;

// 颜色映射
const categoryColors = {
  '市': '#0F9D7A',
  '景点': '#3B82F6',
  '美食': '#F59E0B',
  '酒店': '#8B5CF6',
  '默认': '#6B7280'
};

// 获取节点颜色
const getNodeColor = (node) => {
  return categoryColors[node.category] || categoryColors['默认'];
};

// 获取节点半径
const getNodeRadius = (node) => {
  return node.category === '市' ? 45 : 28;
};

// 初始化 SVG 和力导向布局
const initCanvas = () => {
  if (!svgRef.value) return;

  const container = svgRef.value.parentElement;
  const width = container.clientWidth;
  const height = container.clientHeight;

  // 创建 SVG
  svg = d3.select(svgRef.value)
    .attr('width', width)
    .attr('height', height);

  // 清空之前的内容
  svg.selectAll('*').remove();

  // 定义箭头标记
  svg.append('defs').selectAll('marker')
    .data(['arrow'])
    .enter().append('marker')
    .attr('id', 'arrow')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 20)
    .attr('refY', 0)
    .attr('markerWidth', 6)
    .attr('markerHeight', 6)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M0,-5L10,0L0,5')
    .attr('fill', '#9CA3AF');

  // 创建容器组
  g = svg.append('g');

  // 添加缩放和拖拽
  zoom = d3.zoom()
    .scaleExtent([0.5, 3])
    .on('zoom', (event) => {
      g.attr('transform', event.transform);
    });

  svg.call(zoom);

  // 创建力导向模拟
  simulation = d3.forceSimulation()
    .force('charge', d3.forceManyBody().strength(-300))
    .force('center', d3.forceCenter(width / 2, height / 2).strength(0.1))
    .force('collision', d3.forceCollide().radius(d => getNodeRadius(d) + 10))
    .force('link', d3.forceLink().id(d => d.id).distance(150))
    .on('tick', ticked);
};

// 更新图谱
const updateGraph = () => {
  if (!g || !simulation) return;

  // 准备数据
  const nodes = props.nodes.map(n => ({
    ...n,
    dimmed: n.dimmed || false
  }));

  const links = props.edges.map(e => ({
    ...e,
    source: e.fromNodeId,
    target: e.toNodeId
  }));

  // 更新连接线
  linkElements = g.selectAll('.kg-link')
    .data(links, d => d.id);

  linkElements.exit().remove();

  const linkEnter = linkElements.enter()
    .append('line')
    .attr('class', 'kg-link')
    .attr('stroke', '#9CA3AF')
    .attr('stroke-width', 1)
    .attr('stroke-opacity', 0.4)
    .attr('marker-end', 'url(#arrow)');

  linkElements = linkEnter.merge(linkElements);

  // 更新节点
  nodeElements = g.selectAll('.kg-node')
    .data(nodes, d => d.id);

  nodeElements.exit().remove();

  const nodeEnter = nodeElements.enter()
    .append('g')
    .attr('class', 'kg-node')
    .call(d3.drag()
      .on('start', dragStarted)
      .on('drag', dragged)
      .on('end', dragEnded))
    .on('click', (event, d) => {
      event.stopPropagation();
      emit('node-click', d);
    })
    .on('mouseenter', (event, d) => {
      emit('node-hover', d);
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation();
      emit('node-click', d);
    });

  // 添加圆形
  nodeEnter.append('circle')
    .attr('r', d => getNodeRadius(d))
    .attr('fill', d => getNodeColor(d))
    .attr('stroke', d => getNodeColor(d))
    .attr('stroke-width', 2)
    .style('filter', 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))');

  // 添加文本标签
  nodeEnter.append('text')
    .attr('class', 'kg-node-label')
    .attr('text-anchor', 'middle')
    .attr('dy', d => d.category === '市' ? 5 : 35)
    .attr('font-size', d => d.category === '市' ? '14px' : '12px')
    .attr('font-weight', d => d.category === '市' ? 'bold' : 'normal')
    .attr('fill', d => d.category === '市' ? '#fff' : '#1F2937')
    .text(d => d.name);

  nodeElements = nodeEnter.merge(nodeElements);

  // 更新节点样式
  nodeElements.select('circle')
    .attr('r', d => getNodeRadius(d))
    .attr('fill', d => getNodeColor(d))
    .attr('stroke', d => d.id === props.selectedNodeId ? '#000' : getNodeColor(d))
    .attr('stroke-width', d => d.id === props.selectedNodeId ? 3 : 2)
    .attr('opacity', d => d.dimmed ? 0.3 : 1)
    .style('cursor', 'pointer')
    .transition()
    .duration(300);

  nodeElements.select('text')
    .attr('opacity', d => d.dimmed ? 0.3 : 1);

  // 更新力导向模拟
  simulation.nodes(nodes);
  simulation.force('link').links(links);
  simulation.alpha(0.3).restart();
};

// 力导向模拟的 tick 函数
const ticked = () => {
  if (linkElements) {
    linkElements
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y);
  }

  if (nodeElements) {
    nodeElements.attr('transform', d => `translate(${d.x},${d.y})`);
  }
};

// 拖拽事件处理
const dragStarted = (event, d) => {
  if (!event.active) simulation.alphaTarget(0.3).restart();
  d.fx = d.x;
  d.fy = d.y;
};

const dragged = (event, d) => {
  d.fx = event.x;
  d.fy = event.y;
};

const dragEnded = (event, d) => {
  if (!event.active) simulation.alphaTarget(0);
  d.fx = null;
  d.fy = null;
};

// 画布点击事件
const handleCanvasClick = (event) => {
  if (event.target === svgRef.value) {
    emit('canvas-click');
  }
};

// 重置视图
const resetView = () => {
  if (!svg || !zoom) return;
  svg.transition()
    .duration(750)
    .call(zoom.transform, d3.zoomIdentity);
};

// 缩放控制
const zoomIn = () => {
  if (!svg || !zoom) return;
  svg.transition()
    .duration(300)
    .call(zoom.scaleBy, 1.2);
};

const zoomOut = () => {
  if (!svg || !zoom) return;
  svg.transition()
    .duration(300)
    .call(zoom.scaleBy, 0.8);
};

// 暴露方法给父组件
defineExpose({
  resetView,
  zoomIn,
  zoomOut
});

// 生命周期
onMounted(() => {
  initCanvas();
  updateGraph();

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    initCanvas();
    updateGraph();
  });
});

// 监听数据变化
watch(() => [props.nodes, props.edges, props.selectedNodeId, props.viewMode], () => {
  nextTick(() => {
    updateGraph();
  });
}, { deep: true });
</script>

<style scoped>
.kg-canvas-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #f9fafb;
}

.kg-canvas {
  width: 100%;
  height: 100%;
  cursor: grab;
}

.kg-canvas:active {
  cursor: grabbing;
}

.kg-node {
  cursor: pointer;
  transition: all 0.3s ease;
}

.kg-node:hover circle {
  filter: drop-shadow(0 4px 12px rgba(0,0,0,0.15));
  transform: scale(1.1);
}

.kg-node-label {
  pointer-events: none;
  user-select: none;
}

.kg-link {
  transition: all 0.3s ease;
}

.kg-link:hover {
  stroke-width: 2;
  stroke-opacity: 1;
}
</style>
