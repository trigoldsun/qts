export interface Position {
  positionId: string;
  accountId: string;
  symbol: string;
  quantity: number;
  avgPrice: number;
  unrealizedPnl: number;
  realizedPnl: number;
  updatedAt: Date;
}

export interface GetPositionsRequest {
  accountId: string;
}
