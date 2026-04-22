export interface Trade {
  tradeId: string;
  orderId: string;
  accountId: string;
  symbol: string;
  side: string;
  price: number;
  quantity: number;
  timestamp: Date;
}

export interface GetTradesRequest {
  accountId: string;
  startTime?: Date;
  endTime?: Date;
}
