<template>
  <div class="trading-panel" :class="{ 'tablet-mode': isTablet, 'mobile-mode': isMobile }">
    <!-- Header with account info -->
    <div class="panel-header">
      <div class="account-info">
        <h2>交易面板</h2>
        <span class="account-id">账户: {{ accountId }}</span>
      </div>
      <div class="connection-status" :class="{ connected: wsConnected }">
        <span class="status-dot"></span>
        <span class="hide-mobile">{{ wsConnected ? '实时连接' : '连接中...' }}</span>
        <span class="show-mobile-only">{{ wsConnected ? '已连接' : '连接中' }}</span>
      </div>
      <div class="header-actions">
        <ThemeToggle />
      </div>
    </div>

    <!-- Top Section: Assets + Market Overview -->
    <div class="top-section">
      <!-- Asset Display -->
      <div class="assets-section">
        <h3>资金信息</h3>
        <div class="assets-grid" v-if="assets.length > 0">
          <div v-for="asset in assets" :key="asset.assetId" class="asset-card">
            <div class="asset-currency">{{ asset.currency }}</div>
            <div class="asset-available">{{ formatNumber(asset.available) }}</div>
            <div class="asset-locked info-compact" v-if="asset.locked > 0">
              冻结: {{ formatNumber(asset.locked) }}
            </div>
          </div>
        </div>
        <div class="loading" v-else>加载资金...</div>
      </div>

      <!-- Market Summary Component -->
      <MarketSummary 
        :symbols="monitoredSymbols"
        :selected-symbol="selectedSymbol"
        :quotes="quotes"
        @select="handleSelectMarket"
      />
    </div>

    <!-- Middle Section: Positions + Quick Order -->
    <div class="middle-section">
      <!-- Position Table Component -->
      <PositionTable 
        :positions="positions"
        :quotes="quotes"
        @close="handleClosePosition"
      />

      <!-- Order Form Component -->
      <OrderForm 
        :selected-symbol="orderForm.symbol"
        :current-quote="currentQuote"
        :assets="assets"
        :positions="positions"
        :is-mobile="isMobile"
        :mobile-expanded="mobileOrderExpanded"
        @submit="handleSubmitOrder"
        @update:mobileExpanded="mobileOrderExpanded = $event"
      />
    </div>

    <!-- Orders Section -->
    <div class="orders-section">
      <OrderTable 
        :orders="orders"
        @cancel="handleCancelOrder"
      />
    </div>

    <!-- Mobile: Floating order button -->
    <button 
      v-if="isMobile && !mobileOrderExpanded"
      class="mobile-sheet-trigger btn-buy touch-target"
      @click="mobileOrderExpanded = true"
    >
      + 下单
    </button>

    <!-- Close Position Dialog -->
    <div class="dialog-overlay" v-if="closeDialog.visible" @click.self="closeDialog.visible = false">
      <div class="dialog">
        <h3>平仓确认</h3>
        <div class="dialog-content">
          <p>确定平掉 {{ closeDialog.position?.symbol }} 的持仓?</p>
          <p>数量: {{ formatNumber(closeDialog.quantity) }}</p>
          <p>预计盈亏: <span :class="closeDialog.position?.unrealizedPnl >= 0 ? 'profit' : 'loss'">
            {{ closeDialog.position?.unrealizedPnl >= 0 ? '+' : '' }}{{ formatNumber(closeDialog.position?.unrealizedPnl || 0) }}
          </span></p>
        </div>
        <div class="dialog-actions">
          <button class="btn-cancel" @click="closeDialog.visible = false">取消</button>
          <button class="btn-sell" @click="confirmClosePosition">确认平仓</button>
        </div>
      </div>
    </div>

    <!-- Confirm Dialog Portal -->
    <ConfirmDialog
      :visible="visible"
      v-bind="options"
      @confirm="handleConfirm"
      @cancel="handleCancel"
      @update:visible="visible = $event"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { io, Socket } from 'socket.io-client'
import { marketApi, tradeApi } from '../utils/api'
import { useConfirm } from '@/composables/useConfirm'
import { useResponsive } from '@/composables/useResponsive'
import ThemeToggle from '@/components/ThemeToggle.vue'
import MarketSummary from '@/components/MarketSummary.vue'
import OrderForm from '@/components/OrderForm.vue'
import PositionTable from '@/components/PositionTable.vue'
import OrderTable from '@/components/OrderTable.vue'
import { formatNumber, formatPrice } from '@/utils/format'

// Responsive state
const { isMobile, isTablet, isDesktop } = useResponsive()

// Confirm dialog for trade operations
const { confirm, ConfirmDialog, handleConfirm, handleCancel } = useConfirm()

// Types
interface Asset {
  assetId: string
  accountId: string
  currency: string
  available: number
  locked: number
  total: number
}

interface Position {
  positionId: string
  accountId: string
  symbol: string
  quantity: number
  avgPrice: number
  unrealizedPnl: number
  realizedPnl: number
}

interface Order {
  orderId: string
  accountId: string
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price: number
  quantity: number
  filledQuantity: number
  status: string
  createdAt: Date
}

interface Quote {
  symbol: string
  price: number
  change: number
  high: number
  low: number
  volume: number
  timestamp: Date
}

// State
const accountId = import.meta.env.VITE_ACCOUNT_ID || 'ACC001'
const assets = ref<Asset[]>([])
const positions = ref<Position[]>([])
const orders = ref<Order[]>([])
const monitoredSymbols = ['BTC-USDT', 'ETH-USDT']
const selectedSymbol = ref('BTC-USDT')
const quotes = ref<Map<string, Quote>>(new Map())
const wsConnected = ref(false)

const orderForm = reactive({
  symbol: 'BTC-USDT',
  side: 'BUY' as 'BUY' | 'SELL',
  type: 'LIMIT' as 'LIMIT' | 'MARKET',
  price: 0,
  quantity: 0,
})

const closeDialog = reactive({
  visible: false,
  position: null as Position | null,
  quantity: 0,
})

// Mobile order form state
const mobileOrderExpanded = ref(false)

let socket: Socket | null = null
let priceUpdateTimer: number | null = null

// Computed
const currentQuote = computed(() => quotes.value.get(selectedSymbol.value))

// Methods
const formatTime = (date: Date): string => {
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleSelectMarket = (symbol: string) => {
  selectedSymbol.value = symbol
  orderForm.symbol = symbol
  fetchQuote(symbol)
}

const handleSubmitOrder = async (orderData: {
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price: number
  quantity: number
}) => {
  // Use confirm dialog for order confirmation
  const result = await confirm({
    title: orderData.side === 'BUY' ? '确认买入' : '确认卖出',
    message: `确定要${orderData.side === 'BUY' ? '买入' : '卖出'} ${orderData.symbol} 吗？`,
    type: orderData.side === 'BUY' ? 'info' : 'warning',
    confirmText: orderData.side === 'BUY' ? '确认买入' : '确认卖出',
    cancelText: '取消',
    detail: {
      '交易品种': orderData.symbol,
      '方向': orderData.side === 'BUY' ? '买入' : '卖出',
      '订单类型': orderData.type === 'LIMIT' ? '限价' : '市价',
      '价格': orderData.type === 'MARKET' ? (currentQuote.value?.price || 0).toFixed(2) : orderData.price.toFixed(2),
      '数量': orderData.quantity.toString(),
      '预估金额': (orderData.quantity * (orderData.type === 'MARKET' ? (currentQuote.value?.price || 0) : orderData.price)).toFixed(2) + ' USDT',
    }
  })

  if (!result) {
    return
  }

  try {
    await submitOrderToApi(orderData)
    orderForm.quantity = 0
    if (orderForm.type === 'LIMIT') {
      orderForm.price = 0
    }
    await Promise.all([fetchAssets(), fetchPositions(), fetchOrders()])
  } catch (error: any) {
    console.error('Order failed:', error)
  }
}

const handleClosePosition = (position: Position) => {
  closeDialog.position = position
  closeDialog.quantity = position.quantity
  closeDialog.visible = true
}

const confirmClosePosition = async () => {
  if (!closeDialog.position) return

  const q = quotes.value.get(closeDialog.position.symbol)
  const result = await confirm({
    title: '确认平仓',
    message: '此操作不可逆，确定要平掉该持仓吗？',
    type: 'danger',
    confirmText: '确认平仓',
    cancelText: '再想想',
    detail: {
      '持仓ID': closeDialog.position.positionId,
      '交易品种': closeDialog.position.symbol,
      '数量': formatNumber(Math.abs(closeDialog.position.quantity)),
      '当前价格': q ? formatPrice(q.price) : '-',
      '预估盈亏': (closeDialog.position.unrealizedPnl >= 0 ? '+' : '') + formatNumber(closeDialog.position.unrealizedPnl),
    }
  })

  if (!result) {
    return
  }

  try {
    await submitOrderToApi({
      symbol: closeDialog.position.symbol,
      side: closeDialog.position.quantity > 0 ? 'SELL' : 'BUY',
      type: 'MARKET',
      price: q?.price || 0,
      quantity: Math.abs(closeDialog.position.quantity),
    })
    closeDialog.visible = false
    await fetchPositions()
  } catch (e) {
    console.error('Close position failed:', e)
  }
}

const handleCancelOrder = async (orderId: string) => {
  try {
    await tradeApi.cancelOrder(orderId)
    await fetchOrders()
  } catch (error) {
    console.error('Failed to cancel order:', error)
  }
}

// API calls
const fetchAssets = async () => {
  try {
    const response = await tradeApi.getAssets(accountId)
    if (response.success) {
      assets.value = response.data
    }
  } catch (error) {
    console.error('Failed to fetch assets:', error)
  }
}

const fetchPositions = async () => {
  try {
    const response = await tradeApi.getPositions(accountId)
    if (response.success) {
      positions.value = response.data
    }
  } catch (error) {
    console.error('Failed to fetch positions:', error)
  }
}

const fetchOrders = async () => {
  try {
    const response = await tradeApi.getOrders(accountId)
    if (response.success) {
      orders.value = response.data
    }
  } catch (error) {
    console.error('Failed to fetch orders:', error)
  }
}

const fetchQuote = async (symbol: string) => {
  try {
    const response = await marketApi.getQuote(symbol)
    if (response.code === 0) {
      const data = response.data
      quotes.value.set(symbol, {
        symbol: data.symbol,
        price: data.lastPrice,
        change: data.change || 0,
        high: data.highPrice,
        low: data.lowPrice,
        volume: data.volume,
        timestamp: new Date(),
      })
    }
  } catch (error) {
    console.error(`Failed to fetch quote for ${symbol}:`, error)
  }
}

const submitOrderToApi = async (orderData: {
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price: number
  quantity: number
}) => {
  const response = await tradeApi.placeOrder({
    accountId,
    ...orderData,
  })
  if (!response.success) {
    throw new Error(response.message || '下单失败')
  }
  return response.data
}

// WebSocket
const connectWebSocket = () => {
  const wsUrl = import.meta.env.VITE_WS_URL || window.location.origin
  socket = io(wsUrl, {
    path: '/v1/market/stream',
    transports: ['websocket'],
    reconnection: true,
    reconnectionDelay: 1000,
  })

  socket.on('connect', () => {
    wsConnected.value = true
    console.log('Trading WebSocket connected')

    monitoredSymbols.forEach(sym => {
      socket?.emit('subscribe', { symbol: sym, type: 'quote' })
    })

    socket?.emit('subscribe_trades', { accountId })
  })

  socket.on('error', (error) => { console.error('WebSocket error:', error) })

  socket.on('quote', (data: { symbol: string; lastPrice: number; change: number; high: number; low: number; volume: number }) => {
    quotes.value.set(data.symbol, {
      symbol: data.symbol,
      price: data.lastPrice,
      change: data.change,
      high: data.high,
      low: data.low,
      volume: data.volume,
      timestamp: new Date(),
    })

    if (data.symbol === selectedSymbol.value && orderForm.type === 'LIMIT' && orderForm.price === 0) {
      orderForm.price = data.lastPrice
    }
  })

  socket.on('trade', (data: any) => {
    console.log('Trade update:', data)
    fetchAssets()
    fetchPositions()
    fetchOrders()
  })

  socket.on('order_update', (data: any) => {
    console.log('Order update:', data)
    fetchOrders()
  })

  socket.on('position_update', (data: any) => {
    console.log('Position update:', data)
    fetchPositions()
  })

  socket.on('disconnect', () => {
    wsConnected.value = false
    console.log('Trading WebSocket disconnected')
  })
}

// Simulate real-time quotes for demo
const simulateQuotes = () => {
  const basePrices: Record<string, number> = {
    'BTC-USDT': 50000,
    'ETH-USDT': 3000,
  }

  priceUpdateTimer = window.setInterval(() => {
    monitoredSymbols.forEach(sym => {
      const base = basePrices[sym] || 50000
      const price = base + (Math.random() - 0.5) * 100
      const change = (Math.random() - 0.5) * 5

      quotes.value.set(sym, {
        symbol: sym,
        price: parseFloat(price.toFixed(2)),
        change: parseFloat(change.toFixed(2)),
        high: parseFloat((price * 1.02).toFixed(2)),
        low: parseFloat((price * 0.98).toFixed(2)),
        volume: Math.floor(1000000 + Math.random() * 5000000),
        timestamp: new Date(),
      })
    })
  }, 2000)
}

// Fetch initial quotes for all monitored symbols
const fetchInitialQuotes = async () => {
  for (const sym of monitoredSymbols) {
    await fetchQuote(sym)
  }
}

// Lifecycle
onMounted(async () => {
  await Promise.all([
    fetchAssets(),
    fetchPositions(),
    fetchOrders(),
    fetchInitialQuotes(),
  ])

  connectWebSocket()

  if (quotes.value.size === 0) {
    simulateQuotes()
  }
})

onUnmounted(() => {
  if (socket) {
    socket.disconnect()
    socket = null
  }
  if (priceUpdateTimer) {
    clearInterval(priceUpdateTimer)
  }
})
</script>

<style scoped>
@import '../styles/tokens.css';
@import '../styles/responsive.css';

.trading-panel {
  padding: var(--spacing-md);
  background: var(--bg-primary);
  min-height: 100vh;
  color: var(--text-primary);
  font-family: var(--font-sans);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-md);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-default);
}

.account-info h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.account-id {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.connection-status.connected {
  color: var(--color-success);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-secondary);
}

.connection-status.connected .status-dot {
  background: var(--color-success);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.top-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.assets-section,
.middle-section,
.orders-section {
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  padding: var(--spacing-md);
}

.assets-section h3,
.middle-section h3,
.orders-section h3 {
  margin: 0 0 12px 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-transform: uppercase;
}

.assets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.asset-card {
  background: var(--bg-primary);
  padding: 12px;
  border-radius: var(--radius-sm);
  text-align: center;
}

.asset-currency {
  font-size: var(--font-size-xs);
  color: #D4AF37;
  margin-bottom: 4px;
}

.asset-available {
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--text-primary);
}

.asset-locked {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.middle-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
}

.loading {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
}

.profit { color: var(--color-rise); }
.loss { color: var(--color-fall); }

.btn-sell,
.btn-buy {
  padding: var(--spacing-xs) 12px;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-xs);
  transition: opacity 0.2s;
}

.btn-sell {
  background: var(--color-fall);
  color: var(--text-primary);
}

.btn-buy {
  background: var(--color-rise);
  color: var(--text-primary);
}

.btn-sell:hover,
.btn-buy:hover {
  opacity: 0.8;
}

.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: var(--bg-secondary);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  min-width: 300px;
}

.dialog h3 {
  margin-bottom: var(--spacing-md);
  color: var(--text-primary);
  font-size: var(--font-size-lg);
}

.dialog-content p {
  margin: var(--spacing-sm) 0;
  font-size: var(--font-size-sm);
  color: #ccc;
}

.dialog-actions {
  display: flex;
  gap: var(--spacing-md);
  margin-top: var(--spacing-lg);
}

.dialog-actions button {
  flex: 1;
  padding: 12px;
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: opacity 0.2s;
}

.dialog-actions .btn-cancel {
  background: transparent;
  border: 1px solid var(--text-secondary);
  color: var(--text-secondary);
}

.dialog-actions .btn-cancel:hover {
  background: var(--text-secondary);
  color: var(--text-primary);
}

/* Mobile order form overlay */
.mobile-mode .mobile-sheet-trigger {
  position: fixed;
  bottom: calc(var(--spacing-lg) + env(safe-area-inset-bottom, 0px));
  right: var(--spacing-lg);
  width: 60px;
  height: 60px;
  border-radius: 50%;
  font-size: var(--font-size-sm);
  box-shadow: var(--shadow-lg);
  z-index: 100;
}

/* Ensure no horizontal overflow */
.trading-panel {
  overflow-x: hidden;
}

/* Responsive */
@media (max-width: 767px) {
  .trading-panel {
    padding: var(--spacing-sm);
  }

  .panel-header {
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }

  .account-info {
    flex: 1;
  }

  .account-info h2 {
    font-size: var(--font-size-md);
  }

  .top-section {
    grid-template-columns: 1fr;
    gap: var(--spacing-sm);
  }

  .assets-section,
  .market-section {
    padding: var(--spacing-sm);
  }

  .assets-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .asset-card {
    padding: 8px;
  }

  .asset-available {
    font-size: var(--font-size-md);
  }

  .middle-section {
    grid-template-columns: 1fr;
    gap: var(--spacing-sm);
  }

  .positions-section,
  .order-section {
    padding: var(--spacing-sm);
  }

  .orders-section {
    padding: var(--spacing-sm);
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  .top-section {
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-md);
  }

  .middle-section {
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-md);
  }

  .assets-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>