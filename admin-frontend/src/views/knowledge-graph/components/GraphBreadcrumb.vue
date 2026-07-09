<template>
  <div class="graph-breadcrumb">
    <div class="breadcrumb-items">
      <template v-for="(item, index) in breadcrumbs" :key="index">
        <button
          :class="['breadcrumb-item', { active: item.isActive }]"
          @click="handleNavigate(item.level)"
          :disabled="item.isActive"
        >
          {{ item.label }}
        </button>
        <span v-if="index < breadcrumbs.length - 1" class="breadcrumb-separator">/</span>
      </template>
    </div>
  </div>
</template>

<script setup>
defineProps({
  breadcrumbs: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['navigate'])

function handleNavigate(level) {
  emit('navigate', level)
}
</script>

<style scoped>
.graph-breadcrumb {
  padding: 8px 16px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.breadcrumb-items {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.breadcrumb-item {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #3b82f6;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.breadcrumb-item:hover:not(.active):not(:disabled) {
  background: #dbeafe;
}

.breadcrumb-item.active {
  color: #1f2937;
  cursor: default;
  font-weight: 600;
}

.breadcrumb-item:disabled {
  cursor: default;
}

.breadcrumb-separator {
  color: #9ca3af;
  font-size: 14px;
  user-select: none;
}
</style>
