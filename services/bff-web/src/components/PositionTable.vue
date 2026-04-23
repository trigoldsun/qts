<template>
  <div class="position-table-container">
    <h3>持仓信息</h3>
    <div class="table-scroll" v-if="positions.length > 0">
      <table class="positions-table">
        <thead>
          <tr>
            <th>品种</th>
            <th>数量</th>
            <th class="hide-mobile">均价</th>
            <th>现价</th>
            <th>浮盈亏</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="pos in positions" :key="pos.positionId">
            <td class="symbol-cell">{{ pos.symbol }}</td>
            <td>{{ formatNumber(pos.quantity) }}</td>
            <td class="hide-mobile">{{ formatPrice(pos.avgPrice) }}</td>
            <td>{{ getMarketPrice(pos.symbol) }}</td>
            <td :class="pos.unrealizedPnl >= 0 ? 'profit' : 'loss'" :data-pnl="pos.positionId">
              {{ pos.unrealizedPnl >= 0 ? '+' : '' }}{{ formatNumber(pos.unrealizedPnl) }}
            </td>
            <td>
              <button class="btn-sell touch-target" @click="handleClose(pos)">平仓</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="empty-state" v-else>
      <span>暂无持仓</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatNumber, formatPrice } from '@/utils/format'

interface Quote {
  symbol: string
  price: number
}

interface Position {
  positionId: string
  symbol: string
  quantity: number
  avgPrice: number
  unrealizedPnl: number
}

interface Props {
  positions: Position[]
  quotes: Map<string, Quote>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  close: [position: Position]
}>()

const getMarketPrice = (symbol: string): string => {
  const q = props.quotes.get(symbol)
  return q ? formatPrice(q.price) : '-'
}

const handleClose = (pos: Position) => {
  emit('close', pos)
}
</script>

<style scoped>
@import '../styles/tokens.css';

.position-table-container {
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

.positions-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-xs);
}

.positions-table th {
  text-align: left;
  padding: var(--spacing-sm);
  color: var(--text-secondary);
  font-weight: normal;
  border-bottom: 1px solid var(--border-default);
}

.positions-table td {
  padding: var(--spacing-sm);
  border-bottom: 1px solid #262a3f;
}

.symbol-cell {
  color: #D4AF37;
}

.profit { color: var(--color-rise); }
.loss { color: var(--color-fall); }

.btn-sell {
  padding: var(--spacing-xs) 12px;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-xs);
  transition: opacity 0.2s;
  background: var(--color-fall);
  color: var(--text-primary);
}

.btn-sell:hover {
  opacity: 0.8;
}

.empty-state {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
}

@media (max-width: 767px) {
  .position-table-container {
    padding: var(--spacing-sm);
  }

  .positions-table {
    font-size: 11px;
  }

  .positions-table th,
  .positions-table td {
    padding: 6px 4px;
  }
}
</style>