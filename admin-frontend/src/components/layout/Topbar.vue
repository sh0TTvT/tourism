<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">{{ activeSectionMeta.kicker }}</p>
      <h2>{{ activeSectionMeta.label }}</h2>
    </div>

    <div class="topbar-actions">
      <slot name="actions"></slot>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue';
import { SECTIONS } from '@/constants/sections';
import { useNavigation } from '@/composables/useNavigation';

const { currentSection } = useNavigation();

const activeSectionMeta = computed(
  () => SECTIONS.find((item) => item.key === currentSection.value) || SECTIONS[0]
);
</script>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  padding: 22px 28px 18px;
  border: 0;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 0;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.78);
}

.topbar h2 {
  font-size: 34px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
