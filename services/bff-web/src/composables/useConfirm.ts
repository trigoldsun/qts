import { ref } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

interface ConfirmOptions {
  title: string
  message: string
  type?: 'warning' | 'danger' | 'info'
  confirmText?: string
  cancelText?: string
  detail?: Record<string, string | number>
}

export function useConfirm() {
  const visible = ref(false)
  const options = ref<ConfirmOptions>({
    title: '',
    message: '',
    type: 'warning',
  })

  const resolveRef = ref<((value: boolean) => void) | null>(null)

  const confirm = (opts: ConfirmOptions): Promise<boolean> => {
    options.value = opts
    visible.value = true

    return new Promise((resolve) => {
      resolveRef.value = resolve
    })
  }

  const handleConfirm = () => {
    resolveRef.value?.(true)
    visible.value = false
  }

  const handleCancel = () => {
    resolveRef.value?.(false)
    visible.value = false
  }

  return {
    visible,
    options,
    confirm,
    ConfirmDialog,
    handleConfirm,
    handleCancel,
  }
}