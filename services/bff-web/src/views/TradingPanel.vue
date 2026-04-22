<template>
  <div class="trading-panel">
    <!-- Header with account info -->
    <div class="panel-header">
      <div class="account-info">
        <h2>交易面板</h2>
        <span class="account-id">账户: {{ accountId }}</span>
      </div>
      <div class="connection-status" :class="{ connected: wsConnected }">
        <span class="status-dot"></span>
        {{ wsConnected ? '实时连接' : '连接中...' }}
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
            <div class="asset-locked" v-if="asset.locked > 0">
              冻结: {{ formatNumber(asset.locked) }}
            </div>
          </div>
        </div>
        <div class="loading" v-else>加载资金...</div>
      </div>

      <!-- Market Overview -->
      <div class="market-section">
        <h3>行情监控</h3>
        <div class="market-tabs">
          <button 
            v-for="sym in monitoredSymbols" 
            :key="sym"
            :class="['tab-btn', { active: selectedSymbol === sym }]"
            @click="selectSymbol(sym)"
          >
            {{ sym }}
          </button>
        </div>
        <div class="market-data" v-if="currentQuote">
          <div class="quote-price" :class="priceDirection">
            {{ formatPrice(currentQuote.price) }}
          </div>
          <div class="quote-change" :class="priceDirection">
            <span>{{ currentQuote.change >= 0 ? '+' : '' }}{{ currentQuote.change.toFixed(2) }}%</span>
          </div>
          <div class="quote-details">
            <div>高: {{ formatPrice(currentQuote.high) }}</div>
            <div>低: {{ formatPrice(currentQuote.low) }}</div>
            <div>量: {{ formatVolume(currentQuote.volume) }}</div>
          </div>
        </div>
        <div class="loading" v-else>加载行情...</div>
      </div>
    </div>

    <!-- Middle Section: Positions + Quick Order -->
    <div class="middle-section">
      <!-- Positions Display -->
      <div class="positions-section">
        <h3>持仓信息</h3>
        <table class="positions-table" v-if="positions.length > 0">
          <thead>
            <tr>
              <th>品种</th>
              <th>数量</th>
              <th>均价</th>
              <th>现价</th>
              <th>浮盈亏</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pos in positions" :key="pos.positionId">
              <td class="symbol-cell">{{ pos.symbol }}</td>
              <td>{{ formatNumber(pos.quantity) }}</td>
              <td>{{ formatPrice(pos.avgPrice) }}</td>
              <td>{{ getMarketPrice(pos.symbol) }}</td>
              <td :class="pos.unrealizedPnl >= 0 ? 'profit' : 'loss'">
                {{ pos.unrealizedPnl >= 0 ? '+' : '' }}{{ formatNumber(pos.unrealizedPnl) }}
              </td>
              <td>
                <button class="btn-sell" @click="openCloseDialog(pos)">平仓</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div class="empty-state" v-else>
          <span>暂无持仓</span>
        </div>
      </div>

      <!-- Quick Order Form -->
      <div class="order-section">
        <h3>快捷下单</h3>
        <div class="order-form">
          <div class="symbol-display">
            <span class="label">交易品种</span>
            <span class="value">{{ orderForm.symbol || '请选择' }}</span>
          </div>

          <div class="order-type-selector">
            <button 
              :class="['type-btn', { active: orderForm.side === 'BUY' }]"
              @click="orderForm.side = 'BUY'"
            >
              买入
            </button>
            <button 
              :class="['type-btn', { active: orderForm.side === 'SELL' }]"
              @click="orderForm.side = 'SELL'"
            >
              卖出
            </button>
          </div>

          <div class="order-kind-selector">
            <button 
              :class="['kind-btn', { active: orderForm.type === 'LIMIT' }]"
              @click="orderForm.type = 'LIMIT'"
            >
              限价
            </button>
            <button 
              :class="['kind-btn', { active: orderForm.type === 'MARKET' }]"
              @click="orderForm.type = 'MARKET'"
            >
              市价
            </button>
          </div>

          <div class="form-group" v-if="orderForm.type === 'LIMIT'">
            <label>价格</label>
            <input 
              type="number" 
              v-model.number="orderForm.price"
              :placeholder="currentQuote ? formatPrice(currentQuote.price) : '0'"
              step="0.01"
            />
          </div>

          <div class="form-group">
            <label>数量</label>
            <input 
              type="number" 
              v-model.number="orderForm.quantity"
              placeholder="数量"
              step="0.001"
            />
          </div>

          <div class="quick-amounts">
            <button @click="setQuantity(0.1)">10%</button>
            <button @click="setQuantity(0.25)">25%</button>
            <button @click="setQuantity(0.5)">50%</button>
            <button @click="setQuantity(1)">100%</button>
          </div>

          <div class="order-summary">
            <div>预估金额: <span>{{ estimatedAmount }} USDT</span></div>
            <div>可平数量: <span>{{ closeableQuantity }}</span></div>
          </div>

          <button 
            :class="['btn-submit', orderForm.side === 'BUY' ? 'btn-buy' : 'btn-sell']"
            @click="submitOrder"
            :disabled="submitting"
          >
            {{ submitting ? '提交中...' : (orderForm.side === 'BUY' ? '买入' : '卖出') }}
          </button>

          <div class="error-msg" v-if="errorMsg">{{ errorMsg }}</div>
        </div>
      </div>
    </div>

    <!-- Orders History -->
    <div class="orders-section">
      <h3>今日委托</h3>
      <table class="orders-table" v-if="orders.length > 0">
        <thead>
          <tr>
            <th>时间</th>
            <th>品种</th>
            <th>方向</th>
            <th>类型</th>
            <th>价格</th>
            <th>数量</th>
            <th>已成交</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.orderId">
            <td>{{ formatTime(order.createdAt) }}</td>
            <td class="symbol-cell">{{ order.symbol }}</td>
            <td :class="order.side === 'BUY' ? 'profit' : 'loss'">
              {{ order.side === 'BUY' ? '买入' : '卖出' }}
            </td>
            <td>{{ order.type === 'LIMIT' ? '限价' : '市价' }}</td>
            <td>{{ order.type === 'LIMIT' ? formatPrice(order.price) : '市价' }}</td>
            <td>{{ formatNumber(order.quantity) }}</td>
            <td>{{ formatNumber(order.filledQuantity) }}</td>
            <td>
              <span :class="'status-' + order.status.toLowerCase()">
                {{ statusText(order.status) }}
              </span>
            </td>
            <td>
              <button 
                v-if="canCancel(order.status)" 
                class="btn-cancel"
                @click="cancelOrder(order.orderId)"
              >
                撤单
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="empty-state" v-else>
        <span>暂无委托</span>
      </div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue';
import { io, Socket } from 'socket.io-client';

// Types
interface Asset {
  assetId: string;
  accountId: string;
  currency: string;
  available: number;
  locked: number;
  total: number;
}

interface Position {
  positionId: string;
  accountId: string;
  symbol: string;
  quantity: number;
  avgPrice: number;
  unrealizedPnl: number;
  realizedPnl: number;
}

interface Order {
  orderId: string;
  accountId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  price: number;
  quantity: number;
  filledQuantity: number;
  status: string;
  createdAt: Date;
}

interface Quote {
  symbol: string;
  price: number;
  change: number;
  high: number;
  low: number;
  volume: number;
  timestamp: Date;
}

// State
const accountId = 'ACC001';
const assets = ref<Asset[]>([]);
const positions = ref<Position[]>([]);
const orders = ref<Order[]>([]);
const monitoredSymbols = ['BTC-USDT', 'ETH-USDT'];
const selectedSymbol = ref('BTC-USDT');
const quotes = ref<Map<string, Quote>>(new Map());
const wsConnected = ref(false);
const submitting = ref(false);
const errorMsg = ref('');

const orderForm = reactive({
  symbol: 'BTC-USDT',
  side: 'BUY' as 'BUY' | 'SELL',
  type: 'LIMIT' as 'LIMIT' | 'MARKET',
  price: 0,
  quantity: 0,
});

const closeDialog = reactive({
  visible: false,
  position: null as Position | null,
  quantity: 0,
});

let socket: Socket | null = null;
let priceUpdateTimer: number | null = null;

// Computed
const currentQuote = computed(() => quotes.value.get(selectedSymbol.value));

const priceDirection = computed(() => {
  if (!currentQuote.value) return '';
  return currentQuote.value.change >= 0 ? 'up' : 'down';
});

const estimatedAmount = computed(() => {
  if (orderForm.type === 'MARKET') {
    return (orderForm.quantity * (currentQuote.value?.price || 0)).toFixed(2);
  }
  return (orderForm.quantity * orderForm.price).toFixed(2);
});

const closeableQuantity = computed(() => {
  if (orderForm.side === 'BUY') {
    const usdtAsset = assets.value.find(a => a.currency === 'USDT');
    if (!usdtAsset) return 0;
    const maxQty = orderForm.price > 0 
      ? usdtAsset.available / orderForm.price 
      : 0;
    return maxQty.toFixed(4);
  } else {
    const pos = positions.value.find(p => p.symbol === orderForm.symbol);
    return pos ? pos.quantity.toString() : '0';
  }
});

// Methods
const formatNumber = (num: number): string => {
  return num.toLocaleString('en-US', { maximumFractionDigits: 4 });
};

const formatPrice = (price: number): string => {
  return price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const formatVolume = (vol: number): string => {
  if (vol >= 1e8) return (vol / 1e8).toFixed(2) + '亿';
  if (vol >= 1e4) return (vol / 1e4).toFixed(2) + '万';
  return vol.toFixed(2);
};

const formatTime = (date: Date): string => {
  return new Date(date).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
};

const getMarketPrice = (symbol: string): string => {
  const q = quotes.value.get(symbol);
  return q ? formatPrice(q.price) : '-';
};

const selectSymbol = (symbol: string) => {
  selectedSymbol.value = symbol;
  orderForm.symbol = symbol;
};

const setQuantity = (ratio: number) => {
  if (orderForm.side === 'BUY') {
    const usdtAsset = assets.value.find(a => a.currency === 'USDT');
    if (!usdtAsset || !currentQuote.value) return;
    const maxQty = usdtAsset.available / (orderForm.price || currentQuote.value.price);
    orderForm.quantity = Math.floor(maxQty * ratio * 10000) / 10000;
  } else {
    const pos = positions.value.find(p => p.symbol === orderForm.symbol);
    if (pos) {
      orderForm.quantity = Math.floor(pos.quantity * ratio * 10000) / 10000;
    }
  }
};

const statusText = (status: string): string => {
  const map: Record<string, string> = {
    PENDING: '待报',
    SUBMITTED: '已报',
    PARTIAL_FILLED: '部分成交',
    FILLED: '全部成交',
    CANCELLED: '已撤',
    REJECTED: '拒绝',
  };
  return map[status] || status;
};

const canCancel = (status: string): boolean => {
  return ['PENDING', 'SUBMITTED', 'PARTIAL_FILLED'].includes(status);
};

const openCloseDialog = (pos: Position) => {
  closeDialog.position = pos;
  closeDialog.quantity = pos.quantity;
  closeDialog.visible = true;
};

const confirmClosePosition = async () => {
  if (!closeDialog.position) return;
  
  try {
    const q = quotes.value.get(closeDialog.position.symbol);
    await submitOrderToApi({
      symbol: closeDialog.position.symbol,
      side: closeDialog.position.quantity > 0 ? 'SELL' : 'BUY',
      type: 'MARKET',
      price: q?.price || 0,
      quantity: Math.abs(closeDialog.position.quantity),
    });
    closeDialog.visible = false;
    await fetchPositions();
  } catch (e) {
    errorMsg.value = '平仓失败';
  }
};

// API calls
const fetchAssets = async () => {
  try {
    const response = await fetch(`/v1/accounts/${accountId}/assets`);
    const result = await response.json();
    if (result.success) {
      assets.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch assets:', error);
  }
};

const fetchPositions = async () => {
  try {
    const response = await fetch(`/v1/accounts/${accountId}/positions`);
    const result = await response.json();
    if (result.success) {
      positions.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch positions:', error);
  }
};

const fetchOrders = async () => {
  try {
    const response = await fetch(`/v1/accounts/${accountId}/orders`);
    const result = await response.json();
    if (result.success) {
      orders.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch orders:', error);
  }
};

const submitOrderToApi = async (orderData: {
  symbol: string;
  side: 'BUY' | 'SELL';
  type: 'LIMIT' | 'MARKET';
  price: number;
  quantity: number;
}) => {
  const response = await fetch('/v1/orders', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      accountId,
      ...orderData,
    }),
  });
  const result = await response.json();
  if (!result.success) {
    throw new Error(result.message || '下单失败');
  }
  return result.data;
};

const submitOrder = async () => {
  if (!orderForm.symbol) {
    errorMsg.value = '请选择交易品种';
    return;
  }
  if (orderForm.quantity <= 0) {
    errorMsg.value = '请输入数量';
    return;
  }
  if (orderForm.type === 'LIMIT' && orderForm.price <= 0) {
    errorMsg.value = '请输入价格';
    return;
  }

  errorMsg.value = '';
  submitting.value = true;

  try {
    await submitOrderToApi({
      symbol: orderForm.symbol,
      side: orderForm.side,
      type: orderForm.type,
      price: orderForm.type === 'MARKET' ? (currentQuote.value?.price || 0) : orderForm.price,
      quantity: orderForm.quantity,
    });

    orderForm.quantity = 0;
    if (orderForm.type === 'LIMIT') {
      orderForm.price = 0;
    }
    
    await Promise.all([fetchAssets(), fetchPositions(), fetchOrders()]);
  } catch (error: any) {
    errorMsg.value = error.message || '下单失败';
  } finally {
    submitting.value = false;
  }
};

const cancelOrder = async (orderId: string) => {
  try {
    await fetch(`/v1/orders/${orderId}`, { method: 'DELETE' });
    await fetchOrders();
  } catch (error) {
    console.error('Failed to cancel order:', error);
  }
};

// WebSocket
const connectWebSocket = () => {
  socket = io('/', {
    path: '/v1/market/stream',
    transports: ['websocket'],
    reconnection: true,
    reconnectionDelay: 1000,
  });

  socket.on('connect', () => {
    wsConnected.value = true;
    console.log('Trading WebSocket connected');
    
    // Subscribe to quotes
    monitoredSymbols.forEach(sym => {
      socket?.emit('subscribe_quote', { symbol: sym });
    });
    
    // Subscribe to account updates
    socket?.emit('subscribe_trades', { accountId });
  });

  socket.on('quote', (data: { symbol: string; price: number; change: number; high: number; low: number; volume: number }) => {
    quotes.value.set(data.symbol, {
      ...data,
      timestamp: new Date(),
    });
    
    // Auto-update order price for selected symbol
    if (data.symbol === selectedSymbol.value && orderForm.type === 'LIMIT' && orderForm.price === 0) {
      orderForm.price = data.price;
    }
  });

  socket.on('trade', (data: any) => {
    console.log('Trade update:', data);
    // Refresh data on trade execution
    fetchAssets();
    fetchPositions();
    fetchOrders();
  });

  socket.on('order_update', (data: any) => {
    console.log('Order update:', data);
    fetchOrders();
  });

  socket.on('position_update', (data: any) => {
    console.log('Position update:', data);
    fetchPositions();
  });

  socket.on('disconnect', () => {
    wsConnected.value = false;
    console.log('Trading WebSocket disconnected');
  });
};

// Simulate real-time quotes for demo
const simulateQuotes = () => {
  const basePrices: Record<string, number> = {
    'BTC-USDT': 50000,
    'ETH-USDT': 3000,
  };

  priceUpdateTimer = window.setInterval(() => {
    monitoredSymbols.forEach(sym => {
      const base = basePrices[sym] || 50000;
      const price = base + (Math.random() - 0.5) * 100;
      const change = (Math.random() - 0.5) * 5;
      
      quotes.value.set(sym, {
        symbol: sym,
        price: parseFloat(price.toFixed(2)),
        change: parseFloat(change.toFixed(2)),
        high: parseFloat((price * 1.02).toFixed(2)),
        low: parseFloat((price * 0.98).toFixed(2)),
        volume: Math.floor(1000000 + Math.random() * 5000000),
        timestamp: new Date(),
      });
    });
  }, 2000);
};

// Lifecycle
onMounted(async () => {
  await Promise.all([fetchAssets(), fetchPositions(), fetchOrders()]);
  connectWebSocket();
  simulateQuotes();
});

onUnmounted(() => {
  if (socket) {
    socket.disconnect();
    socket = null;
  }
  if (priceUpdateTimer) {
    clearInterval(priceUpdateTimer);
  }
});
</script>

<style scoped>
.trading-panel {
  padding: 16px;
  background: #1a1a2e;
  min-height: 100vh;
  color: #e0e0e0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #333;
}

.account-info h2 {
  margin: 0;
  font-size: 20px;
  color: #fff;
}

.account-id {
  font-size: 12px;
  color: #888;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #888;
}

.connection-status.connected {
  color: #4caf50;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #888;
}

.connection-status.connected .status-dot {
  background: #4caf50;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.top-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.assets-section,
.market-section,
.positions-section,
.order-section,
.orders-section {
  background: #16213e;
  border-radius: 8px;
  padding: 16px;
}

h3 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #888;
  text-transform: uppercase;
}

.assets-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.asset-card {
  background: #1a1a2e;
  padding: 12px;
  border-radius: 6px;
  text-align: center;
}

.asset-currency {
  font-size: 12px;
  color: #ffd700;
  margin-bottom: 4px;
}

.asset-available {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
}

.asset-locked {
  font-size: 11px;
  color: #888;
  margin-top: 2px;
}

.market-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.tab-btn {
  padding: 6px 16px;
  border: 1px solid #333;
  background: transparent;
  color: #888;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.tab-btn.active {
  background: #e94560;
  border-color: #e94560;
  color: #fff;
}

.market-data {
  text-align: center;
}

.quote-price {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 4px;
}

.quote-price.up { color: #f23645; }
.quote-price.down { color: #089981; }

.quote-change {
  font-size: 14px;
  margin-bottom: 12px;
}

.quote-change.up { color: #f23645; }
.quote-change.down { color: #089981; }

.quote-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  font-size: 12px;
  color: #888;
}

.middle-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.positions-table,
.orders-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.positions-table th,
.orders-table th {
  text-align: left;
  padding: 8px;
  color: #888;
  font-weight: normal;
  border-bottom: 1px solid #333;
}

.positions-table td,
.orders-table td {
  padding: 8px;
  border-bottom: 1px solid #262a3f;
}

.symbol-cell {
  color: #ffd700;
}

.profit { color: #f23645; }
.loss { color: #089981; }

.btn-sell,
.btn-buy {
  padding: 4px 12px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: opacity 0.2s;
}

.btn-sell {
  background: #089981;
  color: #fff;
}

.btn-buy {
  background: #f23645;
  color: #fff;
}

.btn-sell:hover,
.btn-buy:hover {
  opacity: 0.8;
}

.empty-state {
  text-align: center;
  padding: 20px;
  color: #666;
}

.order-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.symbol-display {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: #1a1a2e;
  border-radius: 4px;
}

.symbol-display .label {
  color: #888;
  font-size: 12px;
}

.symbol-display .value {
  color: #ffd700;
  font-weight: bold;
}

.order-type-selector,
.order-kind-selector {
  display: flex;
  gap: 8px;
}

.type-btn,
.kind-btn {
  flex: 1;
  padding: 10px;
  border: 1px solid #333;
  background: transparent;
  color: #888;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.type-btn.active {
  background: #f23645;
  border-color: #f23645;
  color: #fff;
}

.kind-btn.active {
  background: #e94560;
  border-color: #e94560;
  color: #fff;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-group label {
  font-size: 12px;
  color: #888;
}

.form-group input {
  padding: 10px 12px;
  background: #1a1a2e;
  border: 1px solid #333;
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
}

.form-group input:focus {
  outline: none;
  border-color: #e94560;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 6px;
}

.quick-amounts button {
  padding: 6px;
  background: #262a3f;
  border: 1px solid #333;
  border-radius: 4px;
  color: #888;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s;
}

.quick-amounts button:hover {
  background: #333;
  color: #fff;
}

.order-summary {
  background: #1a1a2e;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  color: #888;
}

.order-summary div {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.order-summary span {
  color: #fff;
}

.btn-submit {
  padding: 14px;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-msg {
  color: #f23645;
  font-size: 12px;
  text-align: center;
  padding: 8px;
  background: rgba(242, 54, 69, 0.1);
  border-radius: 4px;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.status-submitted { color: #2196f3; }
.status-partial_filled { color: #ff9800; }
.status-filled { color: #4caf50; }
.status-cancelled { color: #888; }
.status-rejected { color: #f23645; }

.btn-cancel {
  padding: 4px 10px;
  background: transparent;
  border: 1px solid #f23645;
  color: #f23645;
  border-radius: 4px;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f23645;
  color: #fff;
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
  background: #16213e;
  padding: 24px;
  border-radius: 12px;
  min-width: 300px;
}

.dialog h3 {
  margin-bottom: 16px;
  color: #fff;
  font-size: 18px;
}

.dialog-content p {
  margin: 8px 0;
  font-size: 14px;
  color: #ccc;
}

.dialog-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.dialog-actions button {
  flex: 1;
  padding: 12px;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.dialog-actions .btn-cancel {
  background: transparent;
  border: 1px solid #888;
  color: #888;
}

.dialog-actions .btn-cancel:hover {
  background: #888;
  color: #fff;
}
</style>
