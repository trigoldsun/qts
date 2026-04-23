<template>
  <div class="order-form-container" :class="{ 'mobile-expanded': mobileExpanded }">
    <h3 class="hide-mobile">快捷下单</h3>
    <div class="order-form">
      <div class="symbol-display">
        <span class="label">交易品种</span>
        <span class="value">{{ formData.symbol || '请选择' }}</span>
      </div>

      <div class="order-type-selector">
        <button 
          :class="['type-btn', { active: formData.side === 'BUY' }]"
          @click="formData.side = 'BUY'"
        >
          买入
        </button>
        <button 
          :class="['type-btn', { active: formData.side === 'SELL' }]"
          @click="formData.side = 'SELL'"
        >
          卖出
        </button>
      </div>

      <div class="order-kind-selector">
        <button 
          :class="['kind-btn', { active: formData.type === 'LIMIT' }]"
          @click="formData.type = 'LIMIT'"
        >
          限价
        </button>
        <button 
          :class="['kind-btn', { active: formData.type === 'MARKET' }]"
          @click="formData.type = 'MARKET'"
        >
          市价
        </button>
      </div>

      <div class="form-group" v-if="formData.type === 'LIMIT'">
        <label>价格</label>
        <input 
          type="number" 
          v-model.number="formData.price"
          :placeholder="currentQuote ? formatPrice(currentQuote.price) : '0'"
          step="0.01"
          class="touch-target"
        />
      </div>

      <div class="form-group">
        <label>数量</label>
        <input 
          type="number" 
          v-model.number="formData.quantity"
          placeholder="数量"
          step="0.001"
          class="touch-target"
        />
      </div>

      <div class="quick-amounts">
        <button @click="setQuantity(0.1)" class="touch-target">10%</button>
        <button @click="setQuantity(0.25)" class="touch-target">25%</button>
        <button @click="setQuantity(0.5)" class="touch-target">50%</button>
        <button @click="setQuantity(1)" class="touch-target">100%</button>
      </div>

      <div class="order-summary info-compact">
        <div>预估金额: <span>{{ estimatedAmount }} USDT</span></div>
        <div>可平数量: <span>{{ closeableQuantity }}</span></div>
      </div>

      <button 
        :class="['btn-submit', formData.side === 'BUY' ? 'btn-buy' : 'btn-sell', 'touch-target']"
        @click="handleSubmit"
        :disabled="submitting"
      >
        {{ submitting ? '提交中...' : (formData.side === 'BUY' ? '买入' : '卖出') }}
      </button>

      <div class="error-msg" v-if="errorMsg">{{ errorMsg }}</div>
    </div>

    <!-- Mobile: Close button when expanded -->
    <button 
      v-if="isMobile && mobileExpanded" 
      class="btn-close-form hide-tablet"
      @click="mobileExpanded = false"
    >
      ✕ 收起
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, inject } from 'vue'
import { formatPrice, formatNumber } from '@/utils/format'

interface Quote {
  symbol: string
  price: number
  change: number
  high: number
  low: number
  volume: number
  timestamp: Date
}

interface Asset {
  assetId: string
  currency: string
  available: number
  locked: number
}

interface Position {
  positionId: string
  symbol: string
  quantity: number
  unrealizedPnl: number
}

interface Props {
  selectedSymbol: string
  currentQuote: Quote | undefined
  assets: Asset[]
  positions: Position[]
  isMobile: boolean
  mobileExpanded: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  submit: [orderData: {
    symbol: string
    side: 'BUY' | 'SELL'
    type: 'LIMIT' | 'MARKET'
    price: number
    quantity: number
  }]
  'update:mobileExpanded': [value: boolean]
}>()

const submitting = ref(false)
const errorMsg = ref('')

const formData = reactive({
  symbol: props.selectedSymbol,
  side: 'BUY' as 'BUY' | 'SELL',
  type: 'LIMIT' as 'LIMIT' | 'MARKET',
  price: 0,
  quantity: 0,
})

// Update symbol when selectedSymbol changes
import { watch } from 'vue'
watch(() => props.selectedSymbol, (newVal) => {
  formData.symbol = newVal
})

const estimatedAmount = computed(() => {
  if (formData.type === 'MARKET') {
    return (formData.quantity * (props.currentQuote?.price || 0)).toFixed(2)
  }
  return (formData.quantity * formData.price).toFixed(2)
})

const closeableQuantity = computed(() => {
  if (formData.side === 'BUY') {
    const usdtAsset = props.assets.find(a => a.currency === 'USDT')
    if (!usdtAsset) return 0
    const maxQty = formData.price > 0 
      ? usdtAsset.available / formData.price 
      : 0
    return maxQty.toFixed(4)
  } else {
    const pos = props.positions.find(p => p.symbol === formData.symbol)
    return pos ? pos.quantity.toString() : '0'
  }
})

const setQuantity = (ratio: number) => {
  if (formData.side === 'BUY') {
    const usdtAsset = props.assets.find(a => a.currency === 'USDT')
    if (!usdtAsset || !props.currentQuote) return
    const maxQty = usdtAsset.available / (formData.price || props.currentQuote.price)
    formData.quantity = Math.floor(maxQty * ratio * 10000) / 10000
  } else {
    const pos = props.positions.find(p => p.symbol === formData.symbol)
    if (pos) {
      formData.quantity = Math.floor(pos.quantity * ratio * 10000) / 10000
    }
  }
}

const handleSubmit = async () => {
  if (!formData.symbol) {
    errorMsg.value = '请选择交易品种'
    return
  }
  if (formData.quantity <= 0) {
    errorMsg.value = '请输入数量'
    return
  }
  if (formData.type === 'LIMIT' && formData.price <= 0) {
    errorMsg.value = '请输入价格'
    return
  }

  errorMsg.value = ''

  const orderData = {
    symbol: formData.symbol,
    side: formData.side,
    type: formData.type,
    price: formData.type === 'MARKET' ? (props.currentQuote?.price || 0) : formData.price,
    quantity: formData.quantity,
  }

  emit('submit', orderData)
}

// Expose reset method for parent
const resetForm = () => {
  formData.quantity = 0
  if (formData.type === 'LIMIT') {
    formData.price = 0
  }
}

defineExpose({ resetForm })

// Update price when quote changes for LIMIT orders with zero price
import { watch } from 'vue'
watch(() => props.currentQuote?.price, (newPrice) => {
  if (newPrice && formData.type === 'LIMIT' && formData.price === 0) {
    formData.price = newPrice
  }
})
</script>

<style scoped>
@import '../styles/tokens.css';

.order-form-container {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

h3 {
  margin: 0 0 12px 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-transform: uppercase;
}

.order-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.symbol-display {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-sm) 12px;
  background: var(--bg-primary);
  border-radius: var(--radius-sm);
}

.symbol-display .label {
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
}

.symbol-display .value {
  color: #D4AF37;
  font-weight: bold;
}

.order-type-selector,
.order-kind-selector {
  display: flex;
  gap: var(--spacing-sm);
}

.type-btn,
.kind-btn {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border-default);
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all 0.2s;
}

.type-btn.active {
  background: var(--color-rise);
  border-color: var(--color-rise);
  color: var(--text-primary);
}

.kind-btn.active {
  background: var(--color-rise);
  border-color: var(--color-rise);
  color: var(--text-primary);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.form-group label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.form-group input {
  padding: 10px 12px;
  background: var(--bg-primary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}

.form-group input:focus {
  outline: none;
  border-color: var(--color-rise);
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.quick-amounts button {
  padding: 6px;
  background: #262a3f;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s;
}

.quick-amounts button:hover {
  background: var(--border-default);
  color: var(--text-primary);
}

.order-summary {
  background: var(--bg-primary);
  padding: 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.order-summary div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.order-summary span {
  color: var(--text-primary);
}

.btn-submit {
  padding: 14px;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--font-size-md);
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-buy {
  background: var(--color-rise);
  color: var(--text-primary);
}

.btn-sell {
  background: var(--color-fall);
  color: var(--text-primary);
}

.error-msg {
  color: var(--color-rise);
  font-size: var(--font-size-xs);
  text-align: center;
  padding: var(--spacing-sm);
  background: rgba(196, 30, 58, 0.1);
  border-radius: var(--radius-sm);
}

/* Mobile styles */
@media (max-width: 767px) {
  .order-form-container {
    padding: var(--spacing-sm);
  }

  .order-form {
    gap: var(--spacing-sm);
  }

  .form-group input {
    padding: 8px 10px;
  }

  .quick-amounts {
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
  }

  .quick-amounts button {
    padding: 8px 4px;
    font-size: 11px;
  }

  .btn-submit {
    padding: 12px;
    font-size: var(--font-size-md);
  }
}

/* Mobile expanded overlay */
.mobile-mode .order-form-container.mobile-expanded {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--bg-primary);
  z-index: 1000;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  border-radius: 0;
  padding: var(--spacing-lg);
}

.mobile-mode .btn-close-form {
  position: absolute;
  top: var(--spacing-md);
  right: var(--spacing-md);
  background: transparent;
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  cursor: pointer;
}
</style>