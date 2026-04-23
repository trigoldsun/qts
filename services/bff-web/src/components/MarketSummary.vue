<template>
  <div class="market-summary">
    <h3>行情监控</h3>
    <div class="market-tabs scroll-x">
      <button 
        v-for="sym in symbols" 
        :key="sym"
        :class="['tab-btn', { active: selectedSymbol === sym }]"
        @click="selectSymbol(sym)"
      >
        {{ sym }}
      </button>
    </div>
    <div class="market-data" v-if="currentQuote">
      <div class="quote-price" :class="priceDirection" :data-symbol="selectedSymbol">
        {{ formatPrice(currentQuote.price) }}
      </div>
      <div class="quote-change" :class="priceDirection">
        <span>{{ currentQuote.change >= 0 ? '+' : '' }}{{ currentQuote.change.toFixed(2) }}%</span>
      </div>
      <div class="quote-details">
        <div class="hide-mobile">高: {{ formatPrice(currentQuote.high) }}</div>
        <div class="hide-mobile">低: {{ formatPrice(currentQuote.low) }}</div>
        <div>量: {{ formatVolume(currentQuote.volume) }}</div>
      </div>
    </div>
    <div class="loading" v-else>加载行情...</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatPrice, formatVolume } from '@/utils/format'

interface Quote {
  symbol: string
  price: number
  change: number
  high: number
  low: number
  volume: number
  timestamp: Date
}

interface Props {
  symbols: string[]
  selectedSymbol: string
  quotes: Map<string, Quote>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [symbol: string]
}>()

const currentQuote = computed(() => props.quotes.get(props.selectedSymbol))

const priceDirection = computed(() => {
  if (!currentQuote.value) return ''
  return currentQuote.value.change >= 0 ? 'up' : 'down'
})

const selectSymbol = (symbol: string) => {
  emit('select', symbol)
}
</script>

<style scoped>
@import '../styles/tokens.css';

.market-summary {
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

.market-tabs {
  display: flex;
  gap: var(--spacing-sm);
  margin-bottom: 12px;
}

.tab-btn {
  padding: 6px 16px;
  border: 1px solid var(--border-default);
  background: transparent;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-xs);
  transition: all 0.2s;
}

.tab-btn.active {
  background: var(--color-rise);
  border-color: var(--color-rise);
  color: var(--text-primary);
}

.market-data {
  text-align: center;
}

.quote-price {
  font-size: var(--font-size-2xl);
  font-weight: bold;
  margin-bottom: 4px;
}

.quote-price.up { color: var(--color-rise); }
.quote-price.down { color: var(--color-fall); }

.quote-change {
  font-size: var(--font-size-sm);
  margin-bottom: 12px;
}

.quote-change.up { color: var(--color-rise); }
.quote-change.down { color: var(--color-fall); }

.quote-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.loading {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
}

@media (max-width: 767px) {
  .quote-price {
    font-size: var(--font-size-xl);
  }

  .quote-details {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>