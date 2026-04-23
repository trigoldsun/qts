<template>
  <div class="risk-status-panel" :class="{ 
    'critical-flash': isCriticalFlashing, 
    'warning-pulse': isWarningPulsing,
    'tablet-mode': isTablet,
    'mobile-mode': isMobile 
  }">
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
        <div class="card-sub info-compact">风险等级: {{ riskStatus.riskLevel }}</div>
      </div>

      <div class="status-card">
        <div class="card-label">可用资金</div>
        <div class="card-value">
          {{ formatNumber(riskStatus.availableFunds) }} USDT
        </div>
        <div class="card-sub info-compact">持仓市值: {{ formatNumber(riskStatus.positionMarketValue) }} USDT</div>
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
          <span class="alert-time hide-mobile">{{ formatTime(alert.timestamp) }}</span>
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
      <div class="table-scroll" v-if="rules.length > 0">
        <table class="rules-table">
          <thead>
            <tr>
              <th>规则名称</th>
              <th class="hide-mobile">当前值</th>
              <th class="hide-mobile">上限</th>
              <th>使用率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.ruleId">
              <td>{{ rule.ruleName }}</td>
              <td class="hide-mobile">{{ rule.unit === '%' ? rule.current + '%' : formatNumber(rule.current) + ' ' + rule.unit }}</td>
              <td class="hide-mobile">{{ rule.unit === '%' ? rule.limit + '%' : formatNumber(rule.limit) + ' ' + rule.unit }}</td>
              <td>
                <div class="usage-bar">
                  <div class="usage-fill" :style="{ width: getUsagePercent(rule) + '%' }"></div>
                  <span class="usage-text">{{ getUsagePercent(rule) }}%</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="loading" v-else>加载中...</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { io, Socket } from 'socket.io-client';
import { alertSound } from '../utils/alertSound';
import { useResponsive } from '@/composables/useResponsive';

// Responsive state
const { isMobile, isTablet, isDesktop } = useResponsive()

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

interface AlertState {
  level: 'info' | 'warning' | 'critical';
  message: string;
  timestamp: Date;
  acknowledged: boolean;
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
const accountId = import.meta.env.VITE_ACCOUNT_ID || 'ACC001';
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
let criticalFlashInterval: ReturnType<typeof setInterval> | null = null;

// Visual effect states
const isCriticalFlashing = ref(false);
const isWarningPulsing = ref(false);

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

// Alert sound and visual effect methods
const triggerAlertEffects = (alert: RiskAlert) => {
  const level = alert.level;
  
  // Play sound based on alert level
  if (level === AlertLevel.CRITICAL) {
    alertSound.playCriticalAlarm();
    startCriticalFlash();
    sendDesktopNotification('🚨 严重风控告警', alert.message);
  } else if (level === AlertLevel.WARNING) {
    alertSound.play('warning');
    startWarningPulse();
  } else {
    alertSound.play('info');
  }
};

// CRITICAL: Flash background red 3 times at 0.5s interval
const startCriticalFlash = () => {
  if (criticalFlashInterval) return; // Prevent duplicate intervals
  
  isCriticalFlashing.value = true;
  let count = 0;
  criticalFlashInterval = setInterval(() => {
    count++;
    if (count >= 3) {
      stopCriticalFlash();
    }
  }, 500);
};

const stopCriticalFlash = () => {
  if (criticalFlashInterval) {
    clearInterval(criticalFlashInterval);
    criticalFlashInterval = null;
  }
  isCriticalFlashing.value = false;
};

// WARNING: Pulse orange border
const startWarningPulse = () => {
  isWarningPulsing.value = true;
  setTimeout(() => {
    isWarningPulsing.value = false;
  }, 2000);
};

// Desktop notification for critical alerts
const sendDesktopNotification = (title: string, body: string) => {
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(title, { body, urgent: true });
  } else if ('Notification' in window && Notification.permission !== 'denied') {
    Notification.requestPermission().then((permission) => {
      if (permission === 'granted') {
        new Notification(title, { body, urgent: true });
      }
    });
  }
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
    // Trigger alert effects (sound + visual)
    triggerAlertEffects(alert);
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
@import '../styles/tokens.css';
@import '../styles/responsive.css';

.risk-status-panel {
  padding: var(--spacing-lg);
  background: var(--bg-primary);
  min-height: 100vh;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
}

.panel-header h2 {
  margin: 0;
  font-size: var(--font-size-xl);
  color: var(--text-primary);
}

.status-indicator {
  padding: 6px 16px;
  border-radius: var(--radius-full);
  font-weight: bold;
  font-size: var(--font-size-sm);
}

.status-indicator.normal {
  background: rgba(16, 185, 129, 0.2);
  color: var(--color-success);
}

.status-indicator.warning {
  background: rgba(245, 158, 11, 0.2);
  color: var(--color-warning);
}

.status-indicator.danger,
.status-indicator.critical {
  background: rgba(220, 38, 38, 0.2);
  color: var(--color-critical);
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-lg);
}

.status-card {
  background: var(--bg-secondary);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
}

.card-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

.card-value {
  font-size: var(--font-size-2xl);
  font-weight: bold;
  color: var(--text-primary);
}

.card-value.normal {
  color: var(--color-success);
}

.card-value.warning {
  color: var(--color-warning);
}

.card-value.critical {
  color: var(--color-critical);
}

.card-sub {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-top: var(--spacing-xs);
}

.alerts-section,
.limits-section,
.rules-section {
  background: var(--bg-secondary);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--spacing-lg);
}

.alerts-section h3,
.limits-section h3,
.rules-section h3 {
  margin: 0 0 var(--spacing-md) 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.alert-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: var(--radius-sm);
  gap: var(--spacing-md);
}

.alert-item.alert-info {
  background: rgba(74, 159, 255, 0.15);
  border-left: 4px solid var(--color-info);
}

.alert-item.alert-warning {
  background: rgba(245, 158, 11, 0.15);
  border-left: 4px solid var(--color-warning);
}

.alert-item.alert-critical {
  background: rgba(220, 38, 38, 0.15);
  border-left: 4px solid var(--color-critical);
}

.alert-icon {
  font-size: 18px;
}

.alert-message {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.alert-time {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.no-alerts {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-lg);
  color: var(--color-success);
  background: rgba(16, 185, 129, 0.15);
  border-radius: var(--radius-sm);
}

.no-alert-icon {
  font-size: 20px;
}

.limits-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-md);
}

.limit-item {
  text-align: center;
  padding: var(--spacing-md);
  background: var(--bg-elevated);
  border-radius: var(--radius-sm);
}

.limit-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-bottom: var(--spacing-sm);
}

.limit-value {
  font-size: var(--font-size-lg);
  font-weight: bold;
  color: var(--text-primary);
}

.rules-table {
  width: 100%;
  border-collapse: collapse;
}

.rules-table th,
.rules-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-default);
}

.rules-table th {
  font-weight: 600;
  color: var(--text-secondary);
  font-size: var(--font-size-xs);
  text-transform: uppercase;
}

.rules-table td {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.usage-bar {
  position: relative;
  height: 20px;
  background: var(--bg-elevated);
  border-radius: 10px;
  overflow: hidden;
}

.usage-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-success), #8bc34a);
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
  color: var(--text-primary);
}

.loading {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--text-secondary);
}

/* Critical alert: Flash background red */
@keyframes critical-flash {
  0%, 100% { background-color: var(--bg-primary); }
  50% { background-color: rgba(220, 38, 38, 0.3); }
}

.risk-status-panel.critical-flash {
  animation: critical-flash 0.5s ease-in-out 3;
}

/* Warning alert: Pulse orange border */
@keyframes warning-pulse {
  0%, 100% { border-color: transparent; }
  50% { border-color: var(--color-warning); }
}

.risk-status-panel.warning-pulse {
  animation: warning-pulse 0.5s ease-in-out 4;
  border: 3px solid transparent;
  border-radius: var(--radius-md);
}

/* === Responsive Styles === */
@media (max-width: 767px) {
  .risk-status-panel {
    padding: var(--spacing-sm);
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-sm);
  }

  .panel-header h2 {
    font-size: var(--font-size-lg);
  }

  .status-cards {
    grid-template-columns: 1fr;
    gap: var(--spacing-sm);
  }

  .status-card {
    padding: var(--spacing-md);
  }

  .card-value {
    font-size: var(--font-size-xl);
  }

  .alerts-section,
  .limits-section,
  .rules-section {
    padding: var(--spacing-md);
    margin-bottom: var(--spacing-md);
  }

  .limits-grid {
    grid-template-columns: 1fr;
  }

  .limit-item {
    padding: var(--spacing-sm);
  }

  .rules-table th,
  .rules-table td {
    padding: 8px 6px;
    font-size: 11px;
  }
}

/* Tablet specific styles (768px - 1199px) */
@media (min-width: 768px) and (max-width: 1199px) {
  .status-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .limits-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* Ensure no horizontal overflow */
.risk-status-panel {
  overflow-x: hidden;
}
</style>
