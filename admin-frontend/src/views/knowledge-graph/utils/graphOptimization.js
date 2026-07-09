import { quadtree } from 'd3-quadtree'

// 视口裁剪：计算可见节点
export function getVisibleNodes(nodes, transform, viewport) {
  // 防御性检查：确保输入有效
  if (!nodes || !Array.isArray(nodes)) return []

  const { x, y, k } = transform // k = scale
  const padding = 100 // 额外渲染边缘区域

  return nodes.filter(node => {
    const screenX = node.x * k + x
    const screenY = node.y * k + y
    return screenX > -padding &&
           screenX < viewport.width + padding &&
           screenY > -padding &&
           screenY < viewport.height + padding
  })
}

// 计算可见边
export function getVisibleLinks(links, visibleNodeIds) {
  // 防御性检查：确保输入有效
  if (!links || !Array.isArray(links)) return []

  return links.filter(link => {
    const sourceId = link.source.id || link.source
    const targetId = link.target.id || link.target
    return visibleNodeIds.has(sourceId) && visibleNodeIds.has(targetId)
  })
}

// 构建四叉树空间索引
export function buildQuadtree(nodes) {
  return quadtree()
    .x(d => d.x)
    .y(d => d.y)
    .addAll(nodes)
}

// 使用四叉树查找视口内的节点
export function findNodesInViewport(tree, viewport, transform) {
  const { x, y, k } = transform
  const padding = 100

  // 将视口坐标转换为图谱坐标
  const x0 = (-x - padding) / k
  const y0 = (-y - padding) / k
  const x1 = (viewport.width - x + padding) / k
  const y1 = (viewport.height - y + padding) / k

  const visibleNodes = []

  tree.visit((node, x1_, y1_, x2_, y2_) => {
    if (!node.length) {
      // 叶子节点
      const data = node.data
      if (data.x >= x0 && data.x <= x1 && data.y >= y0 && data.y <= y1) {
        visibleNodes.push(data)
      }
    }
    // 如果区域完全在视口外，跳过子树
    return x2_ < x0 || x1_ > x1 || y2_ < y0 || y1_ > y1
  })

  return visibleNodes
}

// 节点池类：复用 DOM 元素
export class NodePool {
  constructor(maxSize = 200) {
    this.pool = []
    this.active = new Map()
    this.maxSize = maxSize
  }

  acquire(nodeId) {
    let element = this.pool.pop()
    if (!element) {
      element = this.createElement()
    }
    this.active.set(nodeId, element)
    return element
  }

  release(nodeId) {
    const element = this.active.get(nodeId)
    if (element) {
      if (this.pool.length < this.maxSize) {
        this.pool.push(element)
      }
      // 始终从 active 中删除，防止内存泄漏
      this.active.delete(nodeId)
    }
  }

  createElement() {
    // 由调用者实现具体的元素创建逻辑
    return document.createElementNS('http://www.w3.org/2000/svg', 'g')
  }

  clear() {
    this.pool = []
    this.active.clear()
  }
}
