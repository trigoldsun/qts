export enum OrderSide {
  BUY = 'BUY',
  SELL = 'SELL',
}

export enum OrderType {
  LIMIT = 'LIMIT',
  MARKET = 'MARKET',
}

export enum OrderStatus {
  PENDING = 'PENDING',
  SUBMITTED = 'SUBMITTED',
  PARTIAL_FILLED = 'PARTIAL_FILLED',
  FILLED = 'FILLED',
  CANCELLED = 'CANCELLED',
  REJECTED = 'REJECTED',
}

export interface Order {
  orderId: string;
  accountId: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  price: number;
  quantity: number;
  filledQuantity: number;
  status: OrderStatus;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateOrderRequest {
  accountId: string;
  symbol: string;
  side: OrderSide;
  type: OrderType;
  price: number;
  quantity: number;
}

export interface AmendOrderRequest {
  orderId: string;
  price?: number;
  quantity?: number;
}
