// 行政层级（可展开）通用配色：城市/省份/国家/区县共用，展开态切换为绿色提示
const levelColor = {
  default: {
    fill: 'rgba(59, 130, 246, 0.12)',
    stroke: '#3b82f6',
    shadow: 'rgba(59, 130, 246, 0.2)'
  },
  expanded: {
    fill: 'rgba(16, 185, 129, 0.12)',
    stroke: '#10b981',
    shadow: 'rgba(16, 185, 129, 0.2)'
  },
  hover: {
    fill: 'rgba(59, 130, 246, 0.2)',
    stroke: '#2563eb'
  }
}

// 节点配色系统
// key 同时支持英文 type 和中文 category：getNodeColor 内部按 type→category 顺序查找
export const nodeColors = {
  // 层级节点（按 category）
  国家: levelColor,
  省份: levelColor,
  城市: levelColor,
  区县: levelColor,
  // 兼容旧英文 type
  city: levelColor,
  // 叶子节点（按 category）
  景点: {
    fill: 'rgba(168, 85, 247, 0.12)',
    stroke: '#a855f7',
    shadow: 'rgba(168, 85, 247, 0.2)'
  },
  attraction: {
    fill: 'rgba(168, 85, 247, 0.12)',
    stroke: '#a855f7',
    shadow: 'rgba(168, 85, 247, 0.2)'
  },
  美食: {
    fill: 'rgba(251, 146, 60, 0.12)',
    stroke: '#fb923c',
    shadow: 'rgba(251, 146, 60, 0.2)'
  },
  小吃: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  点心: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  主菜: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  主食: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  汤羹: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  饮品: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  早餐: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  特产: { fill: 'rgba(251, 146, 60, 0.12)', stroke: '#fb923c', shadow: 'rgba(251, 146, 60, 0.2)' },
  restaurant: {
    fill: 'rgba(251, 146, 60, 0.12)',
    stroke: '#fb923c',
    shadow: 'rgba(251, 146, 60, 0.2)'
  },
  酒店: {
    fill: 'rgba(236, 72, 153, 0.12)',
    stroke: '#ec4899',
    shadow: 'rgba(236, 72, 153, 0.2)'
  },
  hotel: {
    fill: 'rgba(236, 72, 153, 0.12)',
    stroke: '#ec4899',
    shadow: 'rgba(236, 72, 153, 0.2)'
  },
  名人: {
    fill: 'rgba(20, 184, 166, 0.12)',
    stroke: '#14b8a6',
    shadow: 'rgba(20, 184, 166, 0.2)'
  },
  文化: {
    fill: 'rgba(234, 179, 8, 0.12)',
    stroke: '#eab308',
    shadow: 'rgba(234, 179, 8, 0.2)'
  },
  selected: {
    fill: 'rgba(239, 68, 68, 0.15)',
    stroke: '#ef4444',
    shadow: 'rgba(239, 68, 68, 0.3)'
  },
  default: {
    fill: 'rgba(156, 163, 175, 0.12)',
    stroke: '#9ca3af',
    shadow: 'rgba(156, 163, 175, 0.2)'
  }
}

// 行政层级 category（可按子节点数动态调整半径）
const EXPANDABLE_CATEGORIES = new Set(['国家', '省份', '城市', '区县'])

// 节点尺寸配置
export const nodeSizes = {
  city: (childCount = 0) => 30 + Math.min(childCount * 2, 20),
  国家: (childCount = 0) => 34 + Math.min(childCount * 2, 20),
  省份: (childCount = 0) => 32 + Math.min(childCount * 2, 20),
  城市: (childCount = 0) => 30 + Math.min(childCount * 2, 20),
  区县: (childCount = 0) => 28 + Math.min(childCount * 2, 20),
  attraction: 24,
  景点: 24,
  restaurant: 20,
  美食: 20,
  小吃: 20,
  点心: 20,
  主菜: 20,
  主食: 20,
  汤羹: 20,
  饮品: 20,
  早餐: 20,
  特产: 20,
  hotel: 20,
  酒店: 22,
  名人: 22,
  文化: 22,
  default: 20
}

export { EXPANDABLE_CATEGORIES }

// 边样式配置
export const linkStyles = {
  normal: {
    width: 2,
    opacity: 0.6,
    color: '#999'
  },
  highlighted: {
    width: 3,
    opacity: 0.8,
    color: '#666'
  }
}

// 动画配置
export const animations = {
  nodeExpand: {
    duration: 500,
    easing: 'cubic-bezier(0.34, 1.56, 0.64, 1)' // 弹性缓动
  },
  nodeCollapse: {
    duration: 300,
    easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
  },
  zoom: {
    duration: 300,
    easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
  },
  search: {
    duration: 750,
    easing: 'cubic-bezier(0.4, 0, 0.2, 1)'
  }
}

// 根据节点类型获取颜色
// 优先按中文 category 派色（更精确），回退到英文 type
export function getNodeColor(node, state = 'default') {
  const lookup = nodeColors[node.category] || nodeColors[node.type] || nodeColors.default
  const colors = lookup

  if (state === 'selected') {
    return nodeColors.selected
  }

  if (state === 'hover') {
    return colors.hover || colors.default || nodeColors.default
  }

  // 可展开的行政层级节点 + 已展开 → 切换为绿色提示
  if (EXPANDABLE_CATEGORIES.has(node.category) && node.expanded && colors.expanded) {
    return colors.expanded
  }

  return colors.default || colors
}

// 根据节点类型获取半径
export function getNodeRadius(node) {
  // 行政层级节点根据子节点数动态调整半径
  if (EXPANDABLE_CATEGORIES.has(node.category)) {
    const sizer = nodeSizes[node.category]
    if (typeof sizer === 'function') {
      return sizer(node.childCount || 0)
    }
  }
  return nodeSizes[node.category] || nodeSizes[node.type] || nodeSizes.default
}

// 缩短标签文本
export function shortenLabel(text, maxLength = 12) {
  if (text == null) return '' // 只处理 null/undefined
  const str = String(text).trim()
  if (!str) return ''
  return str.length > maxLength ? `${str.slice(0, maxLength)}…` : str
}
