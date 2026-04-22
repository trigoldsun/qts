<template>
  <div class="risk-status-panel">
    <div class="panel-header">
      <h2>风控状态面板</h2>
      <span class="status-indicator" :class="riskLevelClass">{{ riskStatus.riskLevel }}</span>
    </div>

    <!-- 实时持仓/资金占用/保证金率 -->
    <div class="status-cards">
      <div class="status-card">
        <div class="card-label">保证金率</div>
        <div class="card-value" :class="marginRatioClass">
          {{ riskStatus.marginRatio }}%
        </div>
        <div class="card-sub">风险等级: {{ riskStatus.riskLevel }}</div>
      </div>

      <div class="status-card">
        <div class="card-label">可用资金</div>
        <div class="card-value">
          {{ formatNumber(riskStatus.availableFunds) }} USDT
        </div>
        <div class="card-sub">持仓市值: {{ formatNumber(riskStatus.positionMarketValue) }} USDT</div>
      </div>
    </div>

    <!-- 风控告警 (WebSocket实时) -->
    <div class="alerts-section">
      <h3>风控告警</h3>
      <div class="alerts-list" v-if="alerts.length > 0">
        <div
          v-for="(alert, index) in alerts"
          :key="index"
          class="alert-item"
          :class="alertLevelClass(alert.level)"
        >
          <span class="alert-icon">{{ alertLevelIcon(alert.level) }}</span>
          <span class="alert-message">{{ alert.message }}</span>
          <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
        </div>
      </div>
      <div v-else class="no-alerts">
        <span class="no-alert-icon">✓</span>
        <span>暂无告警</span>
      </div>
    </div>

    <!-- 交易限制卡片 -->
    <div class="limits-section">
      <h3>交易限制</h3>
      <div class="limits-grid" v-if="limits">
        <div class="limit-item">
          <div class="limit-label">当日可买额度</div>
          <div class="limit-value">{{ formatNumber(limits.dailyBuyLimit) }} USDT</div>
        </div>
        <div class="limit-item">
          <div class="limit-label">持仓上限</div>
          <div class="limit-value">{{ formatNumber(limits.positionLimit) }} USDT</div>
        </div>
        <div class="limit-item">
          <div class="limit-label">单笔上限</div>
          <div class="limit-value">{{ formatNumber(limits.singleTradeLimit) }} USDT</div>
        </div>
      </div>
      <div class="loading" v-else>加载中...</div>
    </div>

    <!-- 风控规则列表 -->
    <div class="rules-section">
      <h3>风控规则</h3>
      <table class="rules-table" v-if="rules.length > 0">
        <thead>
          <tr>
            <th>规则名称</th>
            <th>当前值</th>
            <th>上限</th>
            <th>使用率</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.ruleId">
            <td>{{ rule.ruleName }}</td>
            <td>{{ rule.unit === '%' ? rule.current + '%' : formatNumber(rule.current) + ' ' + rule.unit }}</td>
            <td>{{ rule.unit === '%' ? rule.limit + '%' : formatNumber(rule.limit) + ' ' + rule.unit }}</td>
            <td>
              <div class="usage-bar">
                <div class="usage-fill" :style="{ width: getUsagePercent(rule) + '%' }"></div>
                <span class="usage-text">{{ getUsagePercent(rule) }}%</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="loading" v-else>加载中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { io, Socket } from 'socket.io-client';

// Types
interface RiskStatus {
  marginRatio: number;
  riskLevel: string;
  availableFunds: number;
  positionMarketValue: number;
}

interface RiskAlert {
  type: string;
  level: number;
  message: string;
  timestamp: string;
}

interface RiskRule {
  ruleId: string;
  ruleName: string;
  limit: number;
  current: number;
  unit: string;
}

interface TradingLimit {
  dailyBuyLimit: number;
  positionLimit: number;
  singleTradeLimit: number;
}

// Alert levels
const AlertLevel = {
  INFO: 1,
  WARNING: 2,
  CRITICAL: 3,
};

// State
const accountId = 'ACC001';
const riskStatus = ref<RiskStatus>({
  marginRatio: 0,
  riskLevel: 'NORMAL',
  availableFunds: 0,
  positionMarketValue: 0,
});
const alerts = ref<RiskAlert[]>([]);
const rules = ref<RiskRule[]>([]);
const limits = ref<TradingLimit | null>(null);
let socket: Socket | null = null;

// Computed
const riskLevelClass = computed(() => {
  return riskStatus.value.riskLevel.toLowerCase();
});

const marginRatioClass = computed(() => {
  if (riskStatus.value.marginRatio > 95) return 'critical';
  if (riskStatus.value.marginRatio > 80) return 'warning';
  return 'normal';
});

// Methods
const formatNumber = (num: number): string => {
  return num.toLocaleString('en-US', { maximumFractionDigits: 2 });
};

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
};

const alertLevelClass = (level: number): string => {
  switch (level) {
    case AlertLevel.CRITICAL:
      return 'alert-critical';
    case AlertLevel.WARNING:
      return 'alert-warning';
    default:
      return 'alert-info';
  }
};

const alertLevelIcon = (level: number): string => {
  switch (level) {
    case AlertLevel.CRITICAL:
      return '⚠️';
    case AlertLevel.WARNING:
      return '⚡';
    default:
      return 'ℹ️';
  }
};

const getUsagePercent = (rule: RiskRule): number => {
  if (rule.limit === 0) return 0;
  return Math.min(100, Math.round((rule.current / rule.limit) * 100));
};

// API calls
const fetchRiskStatus = async () => {
  try {
    const response = await fetch('/api/v1/risk/status', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId }),
    });
    const result = await response.json();
    if (result.success) {
      riskStatus.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch risk status:', error);
  }
};

const fetchRiskRules = async () => {
  try {
    const response = await fetch('/api/v1/risk/rules', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId }),
    });
    const result = await response.json();
    if (result.success) {
      rules.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch risk rules:', error);
  }
};

const fetchTradingLimits = async () => {
  try {
    const response = await fetch('/api/v1/risk/limits', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountId }),
    });
    const result = await response.json();
    if (result.success) {
      limits.value = result.data;
    }
  } catch (error) {
    console.error('Failed to fetch trading limits:', error);
  }
};

// WebSocket connection
const connectWebSocket = () => {
  socket = io('/ws/risk/alerts', {
    path: '/ws/risk/alerts',
    transports: ['websocket'],
  });

  socket.on('connect', () => {
    console.log('Risk WebSocket connected');
    socket?.emit('subscribe_alerts', { accountId });
  });

  socket.on('risk_alert', (alert: RiskAlert) => {
    console.log('Received risk alert:', alert);
    alerts.value.unshift(alert);
    // Keep only last 10 alerts
    if (alerts.value.length > 10) {
      alerts.value = alerts.value.slice(0, 10);
    }
  });

  socket.on('disconnect', () => {
    console.log('Risk WebSocket disconnected');
  });
};

const disconnectWebSocket = () => {
  if (socket) {
    socket.emit('unsubscribe_alerts', { accountId });
    socket.disconnect();
    socket = null;
  }
};

// Lifecycle
onMounted(() => {
  fetchRiskStatus();
  fetchRiskRules();
  fetchTradingLimits();
  connectWebSocket();

  // Auto-refresh risk status every 30 seconds
  const interval = setInterval(fetchRiskStatus, 30000);

  onUnmounted(() => {
    clearInterval(interval);
    disconnectWebSocket();
  });
});
</script>

<style scoped>
.risk-status-panel {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.panel-header h2 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.status-indicator {
  padding: 6px 16px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;
}

.status-indicator.normal {
  background: #e8f5e9;
  color: #2e7d32;
}

.status-indicator.warning {
  background: #fff3e0;
  color: #ef6c00;
}

.status-indicator.danger,
.status-indicator.critical {
  background: #ffebee;
  color: #c62828;
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.status-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.card-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.card-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.card-value.normal {
  color: #2e7d32;
}

.card-value.warning {
  color: #ef6c00;
}

.card-value.critical {
  color: #c62828;
}

.card-sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.alerts-section,
.limits-section,
.rules-section {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.alerts-section h3,
.limits-section h3,
.rules-section h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #333;
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alert-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 6px;
  gap: 12px;
}

.alert-item.alert-info {
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.alert-item.alert-warning {
  background: #fff3e0;
  border-left: 4px solid #ff9800;
}

.alert-item.alert-critical {
  background: #ffebee;
  border-left: 4px solid #f44336;
}

.alert-icon {
  font-size: 18px;
}

.alert-message {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.alert-time {
  font-size: 12px;
  color: #999;
}

.no-alerts {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  color: #4caf50;
  background: #e8f5e9;
  border-radius: 6px;
}

.no-alert-icon {
  font-size: 20px;
}

.limits-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.limit-item {
  text-align: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
}

.limit-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.limit-value {
  font-size: 18px;
  font-weight: bold;
  color: #333;
}

.rules-table {
  width: 100%;
  border-collapse: collapse;
}

.rules-table th,
.rules-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.rules-table th {
  font-weight: 600;
  color: #666;
  font-size: 12px;
  text-transform: uppercase;
}

.rules-table td {
  font-size: 14px;
  color: #333;
}

.usage-bar {
  position: relative;
  height: 20px;
  background: #e0e0e0;
  border-radius: 10px;
  overflow: hidden;
}

.usage-fill {
  height: 100%;
  background: linear-gradient(90deg, #4caf50, #8bc34a);
  border-radius: 10px;
  transition: width 0.3s ease;
}

.usage-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 11px;
  font-weight: bold;
  color: #333;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #999;
}
</style>
