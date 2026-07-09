<template>
  <svg
    :width="size"
    :height="size"
    :viewBox="viewBox"
    :stroke="color"
    fill="none"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
  >
    <path
      v-for="(path, index) in paths"
      :key="index"
      :d="path"
    />
  </svg>
</template>

<script setup>
import { computed } from 'vue'
import { ICON_PATHS } from '@/constants'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  size: {
    type: [Number, String],
    default: 24
  },
  color: {
    type: String,
    default: 'currentColor'
  }
})

const paths = computed(() => {
  const iconData = ICON_PATHS[props.name]
  if (!iconData) return []
  return Array.isArray(iconData) ? iconData : [iconData]
})
const viewBox = computed(() => `0 0 24 24`)
</script>
