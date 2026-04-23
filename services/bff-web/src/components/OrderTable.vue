<template>
  <div class="order-table-container">
    <h3>今日委托</h3>
    <div class="table-scroll" v-if="orders.length > 0">
      <table class="orders-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>品种</th>
            <th class="hide-mobile">方向</th>
            <th class="hide-mobile">类型</th>
            <th>价格</th>
            <th>数量</th>
            <th class="hide-tablet">已成交</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.orderId" :data-order="order.orderId">
            <td>{{ formatTime(order.createdAt) }}</td>
            <td class="symbol-cell">{{ order.symbol }}</td>
            <td class="hide-mobile" :class="order.side === 'BUY' ? 'profit' : 'loss'">
              {{ order.side === 'BUY' ? '买入' : '卖出' }}
            </td>
            <td class="hide-mobile">{{ order.type === 'LIMIT' ? '限价' : '市价' }}</td>
            <td>{{ order.type === 'LIMIT' ? formatPrice(order.price) : '市价' }}</td>
            <td>{{ formatNumber(order.quantity) }}</td>
            <td class="hide-tablet">{{ formatNumber(order.filledQuantity) }}</td>
            <td>
              <span :class="'status-' + order.status.toLowerCase()">
                {{ statusText(order.status) }}
              </span>
            </td>
            <td>
              <button 
                v-if="canCancel(order.status)" 
                class="btn-cancel touch-target"
                @click="handleCancel(order.orderId)"
              >
                撤单
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="empty-state" v-else>
      <span>暂无委托</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatNumber, formatPrice, formatTime } from '@/utils/format'

interface Order {
  orderId: string
  symbol: string
  side: 'BUY' | 'SELL'
  type: 'LIMIT' | 'MARKET'
  price: number
  quantity: number
  filledQuantity: number
  status: string
  createdAt: Date
}

interface Props {
  orders: Order[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  cancel: [orderId: string]
}>()

const statusText = (status: string): string => {
  const map: Record<string, string> = {
    PENDING: '待报',
    SUBMITTED: '已报',
    PARTIAL_FILLED: '部分成交',
    FILLED: '全部成交',
    CANCELLED: '已撤',
    REJECTED: '拒绝',
  }
  return map[status] || status
}

const canCancel = (status: string): boolean => {
  return ['PENDING', 'SUBMITTED', 'PARTIAL_FILLED'].includes(status)
}

const handleCancel = (orderId: string) => {
  emit('cancel', orderId)
}
</script>

<style scoped>
@import '../styles/tokens.css';

.order-table-container {
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

.orders-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-xs);
}

.orders-table th {
  text-align: left;
  padding: var(--spacing-sm);
  color: var(--text-secondary);
  font-weight: normal;
  border-bottom: 1px solid var(--border-default);
}

.orders-table td {
  padding: var(--spacing-sm);
  border-bottom: 1px solid #262a3f;
}

.symbol-cell {
  color: #D4AF37;
}

.profit { color: var(--color-rise); }
.loss { color: var(--color-fall); }

.status-submitted { color: var(--color-info); }
.status-partial_filled { color: var(--color-warning); }
.status-filled { color: var(--color-success); }
.status-cancelled { color: var(--text-secondary); }
.status-rejected { color: var(--color-rise); }

.btn-cancel {
  padding: var(--spacing-xs) 10px;
  background: transparent;
  border: 1px solid var(--color-rise);
  color: var(--color-rise);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 11px;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: var(--color-rise);
  color: var(--text-primary);
}

.empty-state {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
}

@media (max-width: 767px) {
  .order-table-container {
    padding: var(--spacing-sm);
  }

  .orders-table {
    font-size: 11px;
  }

  .orders-table th,
  .orders-table td {
    padding: 6px 4px;
  }
}
</style>