<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="confirm-overlay" @click.self="handleCancel">
        <div class="confirm-dialog" :class="[type]">
          <div class="confirm-header">
            <span class="confirm-icon">{{ icon }}</span>
            <span class="confirm-title">{{ title }}</span>
          </div>
          <div class="confirm-body">
            <p class="confirm-message">{{ message }}</p>
            <div v-if="detail" class="confirm-detail">
              <div v-for="(value, key) in detail" :key="key" class="detail-row">
                <span class="detail-label">{{ key }}:</span>
                <span class="detail-value">{{ value }}</span>
              </div>
            </div>
          </div>
          <div class="confirm-footer">
            <button class="btn btn-cancel" @click="handleCancel">
              {{ cancelText }}
            </button>
            <button 
              class="btn btn-confirm" 
              :class="[type]"
              @click="handleConfirm"
              ref="confirmBtn"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  visible: boolean
  title: string
  message: string
  type?: 'warning' | 'danger' | 'info'
  confirmText?: string
  cancelText?: string
  detail?: Record<string, string | number>
}

const props = withDefaults(defineProps<Props>(), {
  type: 'warning',
  confirmText: '确认',
  cancelText: '取消',
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  confirm: []
  cancel: []
}>()

const confirmBtn = ref<HTMLButtonElement | null>(null)

const iconMap = {
  warning: '⚠️',
  danger: '🚨',
  info: 'ℹ️',
}

const icon = iconMap[props.type || 'warning']

const handleConfirm = () => {
  emit('confirm')
  emit('update:visible', false)
}

const handleCancel = () => {
  emit('cancel')
  emit('update:visible', false)
}

// ESC键关闭
watch(() => props.visible, (val) => {
  if (val) {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') handleCancel()
    }
    document.addEventListener('keydown', handleEsc)
    return () => document.removeEventListener('keydown', handleEsc)
  }
})
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.confirm-dialog {
  background: #1a1a2e;
  border-radius: 12px;
  padding: 24px;
  min-width: 360px;
  max-width: 480px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.confirm-dialog.warning { border-left: 4px solid #F59E0B; }
.confirm-dialog.danger { border-left: 4px solid #DC2626; }
.confirm-dialog.info { border-left: 4px solid #4A9FFF; }

.confirm-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.confirm-icon { font-size: 24px; }
.confirm-title { font-size: 18px; font-weight: 600; color: #F9FAFB; }

.confirm-body { margin-bottom: 24px; }

.confirm-message {
  color: #D1D5DB;
  font-size: 14px;
  line-height: 1.6;
}

.confirm-detail {
  margin-top: 12px;
  background: rgba(255,255,255,0.05);
  border-radius: 8px;
  padding: 12px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
}

.detail-label { color: #9CA3AF; }
.detail-value { color: #F9FAFB; font-family: monospace; }

.confirm-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: transparent;
  border: 1px solid #4B5563;
  color: #D1D5DB;
}
.btn-cancel:hover { background: rgba(255,255,255,0.05); }

.btn-confirm {
  border: none;
  color: white;
}
.btn-confirm.warning { background: #F59E0B; }
.btn-confirm.danger { background: #DC2626; }
.btn-confirm.info { background: #4A9FFF; }
.btn-confirm:hover { opacity: 0.9; }

/* Transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-active .confirm-dialog, .modal-leave-active .confirm-dialog { transition: transform 0.2s; }
.modal-enter-from .confirm-dialog, .modal-leave-to .confirm-dialog { transform: scale(0.95); }
</style>