import { select, selectAll } from 'd3-selection'
import { getNodeColor, getNodeRadius, shortenLabel } from './graphStyles'

// 渲染节点
export function renderNodes(svg, nodes, options = {}) {
  // 参数验证
  if (!svg || !nodes) {
    console.warn('renderNodes: invalid parameters')
    return null
  }

  const {
    onNodeClick,
    onNodeDoubleClick,
    onNodeMouseEnter,
    onNodeMouseLeave,
    selectedNodeId,
    hoveredNodeId,
    dragBehavior
  } = options

  const nodeGroup = svg.select('.nodes')

  // 数据绑定
  const nodeSelection = nodeGroup
    .selectAll('.node')
    .data(nodes, d => d.id)

  // 移除旧节点（立即移除，无过渡）
  nodeSelection.exit().remove()

  // 创建新节点组（立即显示，无淡入）
  const nodeEnter = nodeSelection.enter()
    .append('g')
    .attr('class', 'node')
    .attr('opacity', 1)
    .style('cursor', 'pointer')

  // 添加节点圆形
  nodeEnter.append('circle')
    .attr('class', 'node-circle')

  // 添加节点标签
  nodeEnter.append('text')
    .attr('class', 'node-label')
    .attr('text-anchor', 'middle')
    .attr('dy', '0.35em')
    .style('pointer-events', 'none')
    .style('user-select', 'none')
    .style('font-size', '12px')
    .style('font-weight', '500')
    .style('fill', '#1f2937')

  // 合并新旧节点
  const nodeMerge = nodeEnter.merge(nodeSelection)

  // 应用拖拽行为
  if (dragBehavior) {
    nodeMerge.call(dragBehavior)
  }

  // 更新节点样式（立即更新，无过渡）
  nodeMerge.select('.node-circle')
    .attr('r', d => getNodeRadius(d))
    .attr('fill', d => {
      const state = d.id === selectedNodeId ? 'selected'
                  : d.id === hoveredNodeId ? 'hover'
                  : 'default'
      return getNodeColor(d, state).fill
    })
    .attr('stroke', d => {
      const state = d.id === selectedNodeId ? 'selected'
                  : d.id === hoveredNodeId ? 'hover'
                  : 'default'
      return getNodeColor(d, state).stroke
    })
    .attr('stroke-width', d => d.id === selectedNodeId ? 3 : 2)
    .style('filter', d => {
      const color = getNodeColor(d, 'default')
      return `drop-shadow(0 2px 8px ${color.shadow})`
    })

  // 更新标签
  nodeMerge.select('.node-label')
    .text(d => shortenLabel(d.name, 12))
    .attr('y', d => getNodeRadius(d) + 16)

  // 事件监听（先移除旧监听器，避免重复绑定）
  nodeMerge
    .on('click', null)
    .on('dblclick', null)
    .on('mouseenter', null)
    .on('mouseleave', null)
    .on('click', (event, d) => {
      event.stopPropagation()
      if (onNodeClick) onNodeClick(d)
    })
    .on('dblclick', (event, d) => {
      event.stopPropagation()
      if (onNodeDoubleClick) onNodeDoubleClick(d)
    })
    .on('mouseenter', (event, d) => {
      if (onNodeMouseEnter) onNodeMouseEnter(d)
    })
    .on('mouseleave', (event, d) => {
      if (onNodeMouseLeave) onNodeMouseLeave(d)
    })

  return nodeMerge
}

// 渲染边
export function renderLinks(svg, links, nodes) {
  // 参数验证
  if (!svg || !links) {
    console.warn('renderLinks: invalid parameters')
    return null
  }

  const linkGroup = svg.select('.links')

  // 数据绑定
  const linkSelection = linkGroup
    .selectAll('.link')
    .data(links, d => {
      const sourceId = typeof d.source === 'object' ? d.source.id : d.source
      const targetId = typeof d.target === 'object' ? d.target.id : d.target
      return `${sourceId}-${targetId}`
    })

  // 移除旧边（立即移除，无过渡）
  linkSelection.exit().remove()

  // 创建新边（立即显示，无淡入）
  const linkEnter = linkSelection.enter()
    .append('line')
    .attr('class', 'link')
    .attr('opacity', 1)
    .attr('stroke', '#999')
    .attr('stroke-width', 2)
    .attr('stroke-opacity', 0.6)

  // 合并新旧边
  const linkMerge = linkEnter.merge(linkSelection)

  return linkMerge
}

// 更新节点和边的位置
export function updatePositions(nodeSelection, linkSelection) {
  // 参数验证
  if (!nodeSelection && !linkSelection) {
    return
  }

  if (nodeSelection) {
    nodeSelection.attr('transform', d => `translate(${d.x},${d.y})`)
  }

  if (linkSelection) {
    linkSelection
      .attr('x1', d => d.source.x)
      .attr('y1', d => d.source.y)
      .attr('x2', d => d.target.x)
      .attr('y2', d => d.target.y)
  }
}

// 初始化 SVG 结构
export function initSvgStructure(svg) {
  // 参数验证
  if (!svg) {
    console.warn('initSvgStructure: invalid svg parameter')
    return null
  }

  const svgSelection = select(svg)

  // 清空现有内容
  svgSelection.selectAll('*').remove()

  // 创建容器组
  const container = svgSelection.append('g')
    .attr('class', 'graph-container')

  // 创建边组（先渲染，在节点下方）
  container.append('g')
    .attr('class', 'links')

  // 创建节点组
  container.append('g')
    .attr('class', 'nodes')

  return container
}

// 高亮节点及其邻居
export function highlightNode(svg, nodeId, links) {
  // 参数验证
  if (!svg || !links) {
    console.warn('highlightNode: invalid parameters')
    return
  }

  const nodeSelection = svg.selectAll('.node')
  const linkSelection = svg.selectAll('.link')

  if (!nodeId) {
    // 取消高亮
    nodeSelection.attr('opacity', 1)
    linkSelection.attr('opacity', 1)
    return
  }

  // 找到相邻节点
  const neighbors = new Set()
  neighbors.add(nodeId)

  links.forEach(link => {
    const sourceId = link.source.id || link.source
    const targetId = link.target.id || link.target

    if (sourceId === nodeId) neighbors.add(targetId)
    if (targetId === nodeId) neighbors.add(sourceId)
  })

  // 降低非相关节点的透明度
  nodeSelection.attr('opacity', d => neighbors.has(d.id) ? 1 : 0.2)

  // 降低非相关边的透明度
  linkSelection.attr('opacity', d => {
    const sourceId = d.source.id || d.source
    const targetId = d.target.id || d.target
    return (sourceId === nodeId || targetId === nodeId) ? 1 : 0.1
  })
}
