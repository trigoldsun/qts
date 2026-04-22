import { BigDecimal } from './common';

/**
 * K线周期
 */
export enum KlinePeriod {
  ONE_MINUTE = '1m',
  FIVE_MINUTES = '5m',
  ONE_HOUR = '1h',
  ONE_DAY = '1d',
}

/**
 * 复权类型
 */
export enum AdjustmentType {
  FORWARD = 'FORWARD',
  BACKWARD = 'BACKWARD',
  NONE = 'NONE',
}

/**
 * 单条K线数据
 */
export interface KlineData {
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  amount: number;
  timestamp: Date;
}

/**
 * K线响应数据
 */
export interface KlineResponse {
  symbol: string;
  period: string;
  klines: KlineData[];
  adjustment: AdjustmentType;
}

/**
 * K线查询请求参数
 */
export interface KlineQueryRequest {
  symbol: string;
  period: KlinePeriod;
  startTime?: Date;
  endTime?: Date;
  count?: number;
  adjustment?: AdjustmentType;
}
