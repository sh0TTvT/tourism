<template>
  <Transition name="toast">
    <div v-if="visible" class="confirm-dialog-overlay" @click="onCancel">
      <section
        class="confirm-dialog"
        role="dialog"
        aria-modal="true"
        :aria-label="title"
        @click.stop
      >
        <button
          type="button"
          class="confirm-dialog-close"
          aria-label="关闭确认弹窗"
          @click="onCancel"
        >
          <AppIcon name="close" />
        </button>

        <div class="confirm-dialog-header">
          <h3>{{ title }}</h3>
        </div>
        <div class="confirm-dialog-body">
          <p>{{ message }}</p>
        </div>
        <div class="confirm-dialog-footer">
          <button class="btn btn-secondary" @click="onCancel">
            {{ cancelText }}
          </button>
          <button class="btn btn-danger" @click="onConfirm">
            {{ confirmText }}
          </button>
        </div>
      </section>
    </div>
  </Transition>
</template>

<script setup>
import { onMounted, onUnmounted, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认操作'
  },
  message: {
    type: String,
    required: true
  },
  confirmText: {
    type: String,
    default: '确认'
  },
  cancelText: {
    type: String,
    default: '取消'
  }
})

const emit = defineEmits(['confirm', 'cancel'])

const onConfirm = () => {
  emit('confirm')
}

const onCancel = () => {
  emit('cancel')
}

const handleEscape = (event) => {
  if (event.key === 'Escape' && props.visible) {
    onCancel()
  }
}

watch(() => props.visible, (newValue) => {
  if (newValue) {
    document.addEventListener('keydown', handleEscape)
  } else {
    document.removeEventListener('keydown', handleEscape)
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.confirm-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.confirm-dialog {
  position: relative;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 400px;
  max-width: 500px;
}

.confirm-dialog-close {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  transition: color 0.2s;
  z-index: 1;
}

.confirm-dialog-close:hover {
  color: #111827;
}

.confirm-dialog-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e5e7eb;
}

.confirm-dialog-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.confirm-dialog-body {
  padding: 24px;
}

.confirm-dialog-body p {
  margin: 0;
  color: #6b7280;
  line-height: 1.5;
}

.confirm-dialog-footer {
  padding: 16px 24px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

/* Toast transition animations */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s ease;
}

.toast-enter-active .confirm-dialog,
.toast-leave-active .confirm-dialog {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
}

.toast-enter-from .confirm-dialog,
.toast-leave-to .confirm-dialog {
  transform: scale(0.9);
  opacity: 0;
}
</style>
