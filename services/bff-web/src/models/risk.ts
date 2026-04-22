// Risk status overview
export interface RiskStatus {
  marginRatio: number;        // 保证金率
  riskLevel: RiskLevel;       // 风控等级
  availableFunds: number;     // 可用资金
  positionMarketValue: number; // 持仓市值
}

export enum RiskLevel {
  NORMAL = 'NORMAL',          // 正常
  WARNING = 'WARNING',        // 预警
  DANGER = 'DANGER',          // 危险
  CRITICAL = 'CRITICAL',      // 强平
}

// Trade pre-check request/response
export interface PrecheckRequest {
  accountId: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  price: number;
  quantity: number;
}

export interface PrecheckResponse {
  canTrade: boolean;
  reasons: string[];
}

// Risk rule
export interface RiskRule {
  ruleId: string;
  ruleName: string;
  limit: number;
  current: number;
  unit: string;
}

// Risk alert (WebSocket message)
export interface RiskAlert {
  type: string;
  level: AlertLevel;
  message: string;
  timestamp: string;
}

export enum AlertLevel {
  INFO = 1,
  WARNING = 2,
  CRITICAL = 3,
}

// Trading limits card
export interface TradingLimit {
  dailyBuyLimit: number;      // 当日可买额度
  positionLimit: number;      // 持仓上限
  singleTradeLimit: number;   // 单笔上限
}
